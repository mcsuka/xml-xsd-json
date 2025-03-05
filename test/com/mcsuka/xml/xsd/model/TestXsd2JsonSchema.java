package com.mcsuka.xml.xsd.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import org.junit.jupiter.api.Test;

public class TestXsd2JsonSchema {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static String json2String(JsonElement o) {
        if (o != null) {
            return GSON.toJson(o);
        } else {
            return "null";
        }
    }

    @Test
    public void testChoice() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Choice.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        System.out.println(n.dumpTree());
        JsonObject jschema = Xsd2JsonSchema.renderSchema(n);
        String result = json2String(jschema);

        System.out.println(result);
    }
}
