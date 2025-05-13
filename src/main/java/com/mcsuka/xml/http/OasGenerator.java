package com.mcsuka.xml.http;

import com.google.gson.*;
import com.mcsuka.xml.json.Xsd2JsonSchema;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.List;

public class OasGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OasGenerator.class.getName());

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    public static JsonObject generateOas(List<SoapRestServiceDefinition> services, String title, String description, String version) {
        JsonObject oas = new JsonObject();
        oas.addProperty("openapi", "3.0.1");
        JsonObject info = new JsonObject();
        info.addProperty("title", title);
        info.addProperty("description", description);
        info.addProperty("version", version);
        oas.add("info", info);

        JsonObject paths = new JsonObject();
        oas.add("paths", paths);

        for (SoapRestServiceDefinition serviceDef: services) {
            JsonObject service = oasService(serviceDef);

            JsonObject optPath = paths.getAsJsonObject(serviceDef.getRestPath());
            JsonObject path = optPath == null ? new JsonObject() : optPath;
            path.add(serviceDef.getRestMethod(), service);

            if (optPath == null) {
                paths.add(serviceDef.getRestPath(), path);
            }
        }

        return oas;
    }

    static JsonObject createJsonObject(@NotNull String name, @NotNull Object value) {
        JsonObject o = new JsonObject();
        switch (value) {
            case String s:
                o.addProperty(name, s);
                break;
            case Character c:
                o.addProperty(name, c);
                break;
            case Number n:
                o.addProperty(name, n);
                break;
            case Boolean b:
                o.addProperty(name, b);
                break;
            case JsonElement j:
                o.add(name, j);
                break;
            default:
                o.addProperty(name, value.toString());
        }
        return o;
    }

    static JsonObject contentWithSchema(JsonObject jsonSchema, String description) {
        JsonObject content = createJsonObject("content",
            createJsonObject("application/json",
                createJsonObject("schema", jsonSchema)));
        content.addProperty("description", description);

        return content;
    }

    static JsonObject oasService(SoapRestServiceDefinition serviceDef) {
        JsonObject service = new JsonObject();
        service.addProperty("description", serviceDef.getDescription());
        JsonArray parameters = new JsonArray();
        serviceDef.getRequestParameters().forEach(paramDef -> {
            parameters.add(paramDef.asOasServiceParam());
        });
        service.add("parameters", parameters);

        serviceDef.getWsdlSource()
            .getOperation(serviceDef.getOperationName())
            .ifPresent(op -> {
                if (serviceDef.getRestMethod().startsWith("p")) { // post, put, patch
                    QName reqRoot = op.requestRootElement();
                    try {
                        SchemaParser p = SchemaParserFactory.newSchemaParser(reqRoot.getNamespaceURI(), serviceDef.getWsdlSource());
                        SchemaNode requestXmlSchema = p.parse(reqRoot.getLocalPart());
                        JsonObject requestJsonSchema = Xsd2JsonSchema.renderElement(requestXmlSchema);
                        service.add("requestBody", contentWithSchema(requestJsonSchema, "Request Body"));
                    } catch (Exception e) {
                        logger.warn("Error parsing request schema for path " + serviceDef.getRestPath(), e);
                        service.add("requestBody", contentWithSchema(createJsonObject("type", "string"), "Request Body"));
                    }
                }
                QName respRoot = op.responseRootElement();
                JsonObject responses = new JsonObject();
                service.add("responses", responses);
                try {
                    SchemaParser p = SchemaParserFactory.newSchemaParser(respRoot.getNamespaceURI(), serviceDef.getWsdlSource());
                    SchemaNode responseXmlSchema = p.parse(respRoot.getLocalPart());
                    JsonObject responseJsonSchema = Xsd2JsonSchema.renderElement(responseXmlSchema);
                    responses.add("200", contentWithSchema(responseJsonSchema, "Success Response"));
                } catch (Exception e) {
                    logger.warn("Error parsing request schema for path " + serviceDef.getRestPath(), e);
                    responses.add("200", contentWithSchema(createJsonObject("type", "string"), "Success Response"));
                }
            });

        return service;
    }


}
