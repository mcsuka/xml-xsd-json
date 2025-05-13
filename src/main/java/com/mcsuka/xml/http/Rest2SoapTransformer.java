package com.mcsuka.xml.http;

import com.google.gson.*;
import com.mcsuka.xml.json.Json2Xml;
import com.mcsuka.xml.json.Xml2Json;
import com.mcsuka.xml.xsd.tools.XmlTools;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.mcsuka.xml.xsd.tools.XmlTools.SOAP_ENVELOPE_NS;

public class Rest2SoapTransformer {

    private final List<SoapRestServiceDefinition> serviceDefs;

    public Rest2SoapTransformer(List<SoapRestServiceDefinition> serviceDefs) {
        this.serviceDefs = serviceDefs;
    }

    public SoapRequest transformRequest(RestRequest restRequest) throws IllegalArgumentException, TransformerException {
        for (SoapRestServiceDefinition serviceDef: serviceDefs) {
            if (serviceDef.match(restRequest)) {

                Json2Xml requestTranslator = serviceDef.getRequestSchema().map(Json2Xml::new)
                    .orElseThrow(() -> new IllegalArgumentException("Could not find WSDL matching request method "
                        + restRequest.method() + " and URI " + restRequest.requestUri()));

                String body = restRequest.body() == null ? "" : restRequest.body();
                JsonObject jsonRoot = body.startsWith("{") || body.startsWith("[")
                    ? (JsonObject)JsonParser.parseString(body)
                    : (JsonObject)JsonParser.parseString("{}");

                if (!serviceDef.getRequestParameters().isEmpty()) {
                    addParamsToJson(restRequest, serviceDef, jsonRoot);
                }

                Document soapRequestBody = requestTranslator.translate(jsonRoot);
                return new SoapRequest(serviceDef, wrapInSoapEnvelope(soapRequestBody));
            }
        }

        throw new IllegalArgumentException("Could not find service matching request method " +
            restRequest.method() + " and URI " + restRequest.requestUri());
    }

    static void addParamsToJson(RestRequest restRequest, SoapRestServiceDefinition serviceDef, JsonObject jsonRoot) {
        Map<String, List<String>> queryParams = restRequest.queryString() == null
            ? Map.of()
            : Arrays.stream(restRequest.queryString().split("&"))
            .map(Rest2SoapTransformer::splitQueryParameter)
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                HashMap::new,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        for (RequestParameter param : serviceDef.getRequestParameters()) {
            if ("path".equals(param.paramType())) {
                String[] path = restRequest.requestUri().split("/");
                serviceDef.getPathParamIndex(param.name())
                    .map(idx -> path[idx])
                    .ifPresent(value -> addValueToJson(jsonRoot, param.jsonPath(), value, param.getOasType()));
            } else if ("query".equals(param.paramType())) {
                Optional.ofNullable(queryParams.get(param.name()))
                    .ifPresent(value -> addValueToJson(jsonRoot, param.jsonPath(), value, param.getOasType(), param.multiValue()));
            } else if ("header".equals(param.paramType())) {
                Optional.ofNullable(restRequest.getHeader(param.name()))
                    .ifPresent(value -> addValueToJson(jsonRoot, param.jsonPath(), value, param.getOasType()));
            }
        }

    }

    static Map.Entry<String, String> splitQueryParameter(String it) {
        final int idx = it.indexOf("=");
        final String key = idx > 0 ? it.substring(0, idx) : it;
        final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : "true";
        return new AbstractMap.SimpleImmutableEntry<>(
            URLDecoder.decode(key, StandardCharsets.UTF_8),
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        );
    }

    static void addValueToJson(JsonObject jsonRoot, String[] jsonPath, String value, String oasType) {
        addValueToJson(jsonRoot, jsonPath, List.of(value), oasType, false);
    }

    static void addValueToJson(JsonObject jsonRoot, String[] jsonPath, List<String> values, String oasType, boolean multiValue) {
        JsonObject currentElem = jsonRoot;
        for (int i = 0; i < jsonPath.length - 1; i++) {
            JsonObject nextElem = currentElem.getAsJsonObject();
            if (nextElem == null) {
                JsonObject x = new JsonObject();
                currentElem.add(jsonPath[i], x);
                currentElem = x;
            } else {
                currentElem = nextElem;
            }
        }
        List<JsonPrimitive> jv = values.stream().map(value -> switch (oasType) {
            case "boolean" -> new JsonPrimitive(Boolean.parseBoolean(value));
            case "integer" -> new JsonPrimitive(Long.parseLong(value));
            case "number" -> new JsonPrimitive(Double.parseDouble(value));
            default -> new JsonPrimitive(value);
        }).toList();
        if (multiValue) {
            JsonArray ja = new JsonArray();
            jv.forEach(ja::add);
            currentElem.add(jsonPath[jsonPath.length - 1], ja);
        } else if (!jv.isEmpty()){
            currentElem.add(jsonPath[jsonPath.length - 1], jv.getFirst());
        }
    }

    static String wrapInSoapEnvelope(Document soapRequestBody) throws TransformerException {
        Element requestMessageRoot = soapRequestBody.getDocumentElement();
        Element soapEnvelope = soapRequestBody.createElementNS(SOAP_ENVELOPE_NS, "SOAP-ENV:Envelope");
        Element soapBody = soapRequestBody.createElementNS(SOAP_ENVELOPE_NS, "SOAP-ENV:Body");
        soapEnvelope.appendChild(soapBody);
        requestMessageRoot.getParentNode().replaceChild(soapEnvelope, requestMessageRoot);
        soapBody.appendChild(requestMessageRoot);

        return XmlTools.renderDOM(soapRequestBody, false);
    }

    private static final Gson GSON = new GsonBuilder()
        .create();

    public RestResponse transformResponse(SoapRestServiceDefinition serviceDef, SoapResponse clientResponse) throws IOException, SAXException, XPathExpressionException {
        Document soapResponseDoc = XmlTools.parseXML(clientResponse.contents());
        Element soapBody = XmlTools.newXPath().evaluateExpression("//SOAP-ENV:Body/*", soapResponseDoc, Element.class);

        if (clientResponse.status() == 200 && soapBody != null) {
            Xml2Json responseTranslator = serviceDef.getResponseSchema()
                .map(schema -> new Xml2Json(true, schema))
                .orElse(new Xml2Json(true));
            JsonElement response = responseTranslator.translate(soapBody);
            if (response.isJsonNull() || response.isJsonObject() && response.getAsJsonObject().isEmpty()) {
                return new RestResponse(404, GSON.toJson(response));
            } else {
                return new RestResponse(200, GSON.toJson(response));
            }
        } else {
            Xml2Json responseTranslator = new Xml2Json(true);
            JsonElement response = responseTranslator.translate(soapBody != null ? soapBody : soapResponseDoc.getDocumentElement());
            return new RestResponse(clientResponse.status(), GSON.toJson(response));
        }
    }

}
