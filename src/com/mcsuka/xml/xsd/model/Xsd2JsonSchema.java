package com.mcsuka.xml.xsd.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Xsd2JsonSchema {

    private static final Logger logger = LoggerFactory.getLogger(Xsd2JsonSchema.class.getName());

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

    static JsonObject newJsonObject(String name, JsonObject value) {
        JsonObject jo = new JsonObject();
        jo.add(name, value);
        return jo;
    }

    static JsonObject renderLeaf(SchemaNode leaf) {
        if (leaf.isAttribute() && leaf.getFixedValue() != null) {
            return null;
        }
        SchemaNode.DataType type = leaf.getW3CType();
        Map<String, String> restrictions = leaf.getRestrictions();
        JsonObject prop = new JsonObject();
        if (type == SchemaNode.DataType.BOOLEAN) {
            prop.addProperty("type", "boolean");
        } else if (type == SchemaNode.DataType.INTEGER || type == SchemaNode.DataType.LONG || type == SchemaNode.DataType.DOUBLE) {
            prop.addProperty("type", type == SchemaNode.DataType.DOUBLE ? "number" : "integer");
            parseNumber(restrictions.get("minInclusive"), type)
                    .ifPresent(n -> prop.addProperty("minimum", n));
            parseNumber(restrictions.get("maxInclusive"), type)
                    .ifPresent(n -> prop.addProperty("maximum", n));
        } else {
            prop.addProperty("type", "string");
            if (type == SchemaNode.DataType.DATE) {
                prop.addProperty("format", "date");
            } else if (type == SchemaNode.DataType.DATETIME) {
                prop.addProperty("format", "date-time");
            } else if (type == SchemaNode.DataType.TIME) {
                prop.addProperty("pattern", "^\\d{2}:\\d{2}(:\\d{2})?$'");
            } else if (restrictions != null) {
                Optional.ofNullable(restrictions.get("pattern"))
                        .ifPresent(p -> prop.addProperty("pattern", p));
                Optional.ofNullable(restrictions.get("enumeration"))
                        .ifPresent(enums -> {
                            JsonArray enumValues = new JsonArray();
                            Arrays.stream(enums.split("[|]")).forEach(enumValues::add);
                            prop.add("enum", enumValues);
                        });
                parseNumber(restrictions.get("length"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> {
                            prop.addProperty("minLength", len);
                            prop.addProperty("maxLength", len);
                        });
                parseNumber(restrictions.get("minLength"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> prop.addProperty("minLength", len));
                parseNumber(restrictions.get("maxLength"), SchemaNode.DataType.INTEGER)
                        .ifPresent(len -> prop.addProperty("maxLength", len));
            }
        }

        if (leaf.getMaxOccurs() > 1) {
            JsonObject array = new JsonObject();
            array.addProperty("type", "array");
            array.add("items", prop);
            return array;
        } else {
            return prop;
        }
    }

    public static JsonObject renderSchema(SchemaNode input) {

        System.out.println(input);

        if (input.isRecursive()) {
            return new JsonObject();
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");

        List<? extends SchemaNode> children = input.getChildren();

        if (input.getIndicator() == SchemaNode.IndicatorType.choice) {
            JsonArray choices = new JsonArray();
            schema.add("oneOf", choices);

            for (SchemaNode child : children) {
                choices.add(renderSchema(child));
            }

        } else {
            JsonObject properties = new JsonObject();
            schema.add("properties", properties);

            if (input.isSimpleType()) {   // XML element with attributes
                JsonObject value = renderLeaf(input);
                properties.add("content", value);
            }

            List<String> required = new ArrayList<>();

            for (SchemaNode child : children) {
                if (child.isIndicator() && child.getIndicator() != SchemaNode.IndicatorType.choice) {
                    JsonObject childProps = renderSchema(child);
                    Optional.ofNullable(childProps.getAsJsonObject("properties"))
                            .ifPresent(props -> props.entrySet()
                                    .forEach(entry -> properties.add(entry.getKey(), entry.getValue())));
                } else {
                    JsonObject prop = child.isLeaf() ? renderLeaf(child) : renderSchema(child);
                    if (prop != null) {
                        properties.add(child.getElementName(), prop);
                        if (child.getMinOccurs() > 0) {
                            required.add(child.getElementName());
                        }
                    }
                }
            }

            if (!required.isEmpty()) {
                JsonArray array = new JsonArray();
                required.forEach(array::add);
                schema.add("required", array);
            }
            if (input.getMaxOccurs() > 1) {
                JsonObject array = new JsonObject();
                array.addProperty("type", "array");
                array.add("items", schema);
                return array;
            }
        }
        return schema;
    }
    
}
