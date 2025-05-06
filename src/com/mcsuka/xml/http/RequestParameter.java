package com.mcsuka.xml.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;

/**
 * OAS service parameter definition
 * @param name parameter name, refers to the path element, query element or header
 * @param paramType the "in" element of an OAS parameter. Possible values: path|query|header
 * @param oasTypeDef defines the JSON Schema type, e.g. type, format, pattern, enum, const, default, minimum, maximum, minLength, naxLength, ...
 * @param multiValue multi value parameter, only valid for query param in OAS 3.0
 * @param required required parameter
 * @param jsonPath JSON element hierarchy to insert this parameter to, which should refer to the target XML element hierarchy
 * @param description free text description
 */
public record RequestParameter(
        String name,
        String paramType,
        Map<String, String> oasTypeDef,
        boolean multiValue,
        boolean required,
        @Nullable String[] jsonPath,
        @Nullable String description
) {

    public RequestParameter {
        if (!paramType.matches("^(path|query|header)$")) {
            throw new IllegalArgumentException("paramType value " + paramType + " is invalid, it must match '^(path|query|header)$'");
        }
        if (jsonPath == null) {
            jsonPath = new String[1];
            jsonPath[0] = name;
        }
    }

    public String getOasType() {
        return oasTypeDef.getOrDefault("type", "string");
    }

    public JsonObject asOasServiceParam() {
        JsonObject paramDef = new JsonObject();
        paramDef.addProperty("name", name);
        paramDef.addProperty("in", paramType);
        if (description != null) {
            paramDef.addProperty("description", description);
        }
        if (required) {
            paramDef.addProperty("required", true);
        }
        JsonObject schema = new JsonObject();
        paramDef.add("schema", schema);
        if (multiValue && "query".equals(paramType)) {
            schema.addProperty("type", "array");
            JsonObject items = new JsonObject();
            oasTypeDef.forEach((k, v) -> items.add(k, schemaParameter(k, v)));
            schema.add("items", items);
        } else {
            oasTypeDef.forEach((k, v) -> schema.add(k, schemaParameter(k, v)));
        }
        return paramDef;
    }

    JsonElement schemaParameter(String name, String value) {
        return switch (name) {
            case "enum": {
                JsonArray a = new JsonArray();
                Arrays.stream(value.split("[|]")).forEach(a::add);
                yield a;
            }
            case "length", "minLength", "maxLength", "minimum", "maximum":
                yield new JsonPrimitive(Integer.parseInt(value));
            default:
                yield new JsonPrimitive(value);
        };
    }

}
