package com.mcsuka.xml.xsd.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.mcsuka.xml.xsd.model.SchemaNode.UNBOUNDED_VALUE;

/**
 * Best-effort translation of an XML Schema to a JSON Schema.
 * Not all XML Schema constructions can be translated into JSON Schema, e.g. JSON does not ensure sequence,
 * cannot have repeating groups of elements or a sequence of choice (oneOf) groups.
 */
public class Xsd2JsonSchema {

    private static final Logger logger = LoggerFactory.getLogger(Xsd2JsonSchema.class.getName());

    public static JsonObject translateSchema(SchemaNode input) {
        JsonObject schema = renderElement(input);
        schema.addProperty("$schema", "https://json-schema.org/draft/2020-12/schema");
        schema.addProperty("$id", input.getNamespace() + "?" + input.getElementName());
        return schema;
    }

    static Optional<Number> parseNumber(String num, SchemaNode.DataType type) {
        if (num == null)
            return Optional.empty();
        try {
            if (type == SchemaNode.DataType.INTEGER) {
                return Optional.of(Integer.valueOf(num));
            } else if (type == SchemaNode.DataType.LONG) {
                return Optional.of(Long.valueOf(num));
            } else if (type == SchemaNode.DataType.DOUBLE) {
                return Optional.of(Double.valueOf(num));
            }
        } catch (NumberFormatException nfe) {
            logger.warn("error parsing numeric value " + num, nfe);
        }
        return Optional.empty();
    }

    @NotNull
    static JsonObject renderLeaf(SchemaNode leaf) {
        SchemaNode.DataType type = leaf.getW3CType();
        Map<String, String> restrictions = leaf.getRestrictions();
        JsonObject typeDef = new JsonObject();
        if (type == SchemaNode.DataType.BOOLEAN) {
            typeDef.addProperty("type", "boolean");
        } else if (type == SchemaNode.DataType.INTEGER || type == SchemaNode.DataType.LONG || type == SchemaNode.DataType.DOUBLE) {
            typeDef.addProperty("type", type == SchemaNode.DataType.DOUBLE ? "number" : "integer");
            parseNumber(restrictions.get("minInclusive"), type)
                    .ifPresent(n -> typeDef.addProperty("minimum", n));
            parseNumber(restrictions.get("maxInclusive"), type)
                    .ifPresent(n -> typeDef.addProperty("maximum", n));
        } else {
            typeDef.addProperty("type", "string");
            if (type == SchemaNode.DataType.DATE) {
                typeDef.addProperty("format", "date");
            } else if (type == SchemaNode.DataType.DATETIME) {
                typeDef.addProperty("format", "date-time");
            } else if (type == SchemaNode.DataType.TIME) {
                typeDef.addProperty("pattern", "^\\d{2}:\\d{2}(:\\d{2})?$'");
            } else if (restrictions != null) {
                Optional.ofNullable(restrictions.get("pattern"))
                        .ifPresent(p -> typeDef.addProperty("pattern", p));
                Optional.ofNullable(restrictions.get("enumeration"))
                        .ifPresent(enums -> {
                            JsonArray enumValues = new JsonArray();
                            Arrays.stream(enums.split("[|]")).forEach(enumValues::add);
                            typeDef.add("enum", enumValues);
                        });
                parseNumber(restrictions.get("length"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> {
                            typeDef.addProperty("minLength", len);
                            typeDef.addProperty("maxLength", len);
                        });
                parseNumber(restrictions.get("minLength"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> typeDef.addProperty("minLength", len));
                parseNumber(restrictions.get("maxLength"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> typeDef.addProperty("maxLength", len));
            }
        }

        if (leaf.getFixedValue() != null) {
            typeDef.addProperty("const", leaf.getFixedValue());
        }

        if (leaf.getDefaultValue() != null) {
            typeDef.addProperty("default", leaf.getDefaultValue());
        }

        if (leaf.getMaxOccurs() > 1) {
            return asArray(leaf, typeDef);
        } else {
            return typeDef;
        }
    }

    @NotNull
    static JsonArray renderOneOf(SchemaNode input) {
        JsonArray choices = new JsonArray();
        List<? extends SchemaNode> children = input.getChildren();

        for (SchemaNode child : children) {
            choices.add(child.isIndicator() ? renderSequence(child) : renderElement(child));
        }
        return choices;
    }

    @NotNull
    static JsonObject renderSequence(SchemaNode input) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject properties = new JsonObject();
        schema.add("properties", properties);
        List<String> required = new ArrayList<>();

        List<? extends SchemaNode> children = input.getChildren();

        for (SchemaNode child : children) {
            if (child.isIndicator()) {
                JsonObject subSeq = renderSequence(child);
                subSeq.getAsJsonObject("properties")
                        .entrySet()
                        .forEach(entry -> properties.add(entry.getKey(), entry.getValue()));
                if (child.getMinOccurs() > 0 && child.getIndicator() != SchemaNode.IndicatorType.choice) {
                    Optional.ofNullable(subSeq.getAsJsonArray("required"))
                            .ifPresent(array -> array.forEach(elem -> required.add(elem.getAsString())));
                }
            } else {
                if (child.isLeaf()) {
                    properties.add(child.getElementName(), renderLeaf(child));
                } else {
                    properties.add(child.getElementName(), renderElement(child));
                }
                if (child.getMinOccurs() > 0) {
                    required.add(child.getElementName());
                }
            }
        }
        if (!required.isEmpty()) {
            JsonArray array = new JsonArray();
            required.forEach(array::add);
            schema.add("required", array);
        }
        if (input.getMaxOccurs() > 1) {
            return asArray(input, schema);
        }
        return schema;

    }

    static JsonObject asArray(SchemaNode input, JsonObject items) {
        JsonObject array = new JsonObject();
        array.addProperty("type", "array");
        array.add("items", items);
        if (input.getMinOccurs() > 0) {
            array.addProperty("minItems", input.getMinOccurs());
        }
        if (input.getMaxOccurs() < UNBOUNDED_VALUE) {
            array.addProperty("maxItems", input.getMaxOccurs());
        }
        return array;

    }

    static JsonObject renderElement(SchemaNode input) {
        if (input.isRecursive()) {
            return new JsonObject();
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        String docs = input.getDocumentation();
        if (docs != null) {
            schema.addProperty("description", docs);
        }
        JsonObject properties = new JsonObject();
        JsonArray choices = new JsonArray();
        JsonObject arraySchema = null;
        List<String> required = new ArrayList<>();

        if (input.isSimpleType()) {   // XML element with attributes
            JsonObject value = renderLeaf(input);
            properties.add("content", value);
        }

        List<? extends SchemaNode> children = input.getChildren();

        for (SchemaNode child : children) {
            if (child.getIndicator() == SchemaNode.IndicatorType.choice) {
                choices.addAll(renderOneOf(child));
            } else if (child.isAttribute()) {
                properties.add(child.getElementName(), renderLeaf(child));
                if (child.getMinOccurs() > 0)
                    required.add(child.getElementName());
            } else {  // sequence or all
                JsonObject seq = renderSequence(child);
                if ("array".equals(seq.get("type").getAsString())) {
                    arraySchema = seq;
                } else {
                    seq.getAsJsonObject("properties").entrySet()
                            .forEach(entry -> properties.add(entry.getKey(), entry.getValue()));
                    Optional.ofNullable(seq.getAsJsonArray("required"))
                            .ifPresent(array -> array.forEach(elem -> required.add(elem.getAsString())));
                }
            }
        }
        if (!choices.isEmpty()) {
            if (properties.isEmpty()) {
                schema.add("oneOf", choices);
            } else {
                JsonObject contents = new JsonObject();
                contents.add("oneOf", choices);
                contents.addProperty("type", "object");
                properties.add("contents", contents);
            }
        }
        if (!properties.isEmpty()) {
            schema.add("properties", properties);
        }
        if (arraySchema != null) {
             if (properties.isEmpty()) {
                return arraySchema;
            } else {
                properties.add("content", arraySchema);
            }
        }
        if (!required.isEmpty()) {
            JsonArray array = new JsonArray();
            required.forEach(array::add);
            schema.add("required", array);
        }
        if (input.getMaxOccurs() > 1) {
            return asArray(input, schema);
        }
        return schema;
    }

}
