package com.mcsuka.xml.http;

import com.google.gson.*;
import com.mcsuka.xml.json.Json2Xml;
import com.mcsuka.xml.xsd.tools.XmlTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.mcsuka.xml.xsd.tools.XmlTools.SOAP_ENVELOPE_NS;

public class Rest2SoapTransformer {

    private final Map<String, SoapRestServiceDefinition> serviceMap;

    public Rest2SoapTransformer(List<SoapRestServiceDefinition> serviceDefs) {
        this.serviceMap = new HashMap<>();
        for(SoapRestServiceDefinition serviceDef: serviceDefs) {
            String pattern = serviceDef.getRestPath().replaceAll("\\{[a-zA-Z0-9_]+\\}", "[a-zA-Z0-9_]+");
            serviceMap.put(pattern, serviceDef);
        }
    }

    public SoapRequest transformRequest(HttpServletRequest servletRequest) throws IllegalArgumentException, IOException, TransformerException {
        String requestUri = servletRequest.getRequestURI();
        String method = servletRequest.getMethod();

        for (Map.Entry<String, SoapRestServiceDefinition> entry: serviceMap.entrySet()) {
            SoapRestServiceDefinition serviceDef = entry.getValue();
            if (method.equals(serviceDef.getRestMethod()) && requestUri.matches(entry.getKey())) {

                Json2Xml requestTranslator = serviceDef.getRequestSchema().map(Json2Xml::new)
                    .orElseThrow(() -> new IllegalArgumentException("Could not find WSDL matching request method " + method + " and URI " + requestUri));

                String body = getRequestBody(servletRequest);
                JsonObject jsonRoot = body.startsWith("{") || body.startsWith("[")
                    ? (JsonObject)JsonParser.parseString(body)
                    : (JsonObject)JsonParser.parseString("{}");

                if (!serviceDef.getRequestParameters().isEmpty()) {
                    addParamsToJson(servletRequest, serviceDef, jsonRoot);
                }

                Document soapRequestBody = requestTranslator.translate(jsonRoot);
                return new SoapRequest(serviceDef.getTargetUrl(), serviceDef.getSoapAction(), wrapInSoapEnvelope(soapRequestBody));
            }
        }

        throw new IllegalArgumentException("Could not find service matching request method " + method + " and URI " + requestUri);
    }

    static String getRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream inputStream = request.getInputStream()) {
            if (inputStream != null) {
                int contentLength = request.getContentLength();
                int size = contentLength > 0 ? contentLength : 1024;
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream(size)) {
                    byte[] buffer = new byte[1024];
                    int len = inputStream.read(buffer);
                    while (len >= 0) {
                        baos.write(buffer, 0, len);
                        len = inputStream.read(buffer);
                    }
                    return baos.toString().trim();
                }
            } else {
                return "";
            }
        }
    }

    static void addParamsToJson(HttpServletRequest servletRequest, SoapRestServiceDefinition serviceDef, JsonObject jsonRoot) {
        String queryString = servletRequest.getQueryString();
        Map<String, List<String>> queryParams = queryString == null
            ? Map.of()
            : Arrays.stream(queryString.split("&"))
            .map(Rest2SoapTransformer::splitQueryParameter)
            .collect(Collectors.groupingBy(
                Map.Entry::getKey,
                HashMap::new,
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        for (RequestParameter param : serviceDef.getRequestParameters()) {
            if ("path".equals(param.paramType())) {
                String[] path = servletRequest.getRequestURI().split("/");
                serviceDef.getPathParamIndex(param.name())
                    .map(idx -> path[idx])
                    .ifPresent(value -> addValueToJson(jsonRoot, param.jsonPath(), value, param.getOasType()));
            } else if ("query".equals(param.paramType())) {
                Optional.ofNullable(queryParams.get(param.name()))
                    .ifPresent(value -> addValueToJson(jsonRoot, param.jsonPath(), value, param.getOasType(), param.multiValue()));
            } else if ("header".equals(param.paramType())) {
                Optional.ofNullable(servletRequest.getHeader(param.name()))
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



    public void transformResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse, SoapResponse clientResponse) {

    }

}
