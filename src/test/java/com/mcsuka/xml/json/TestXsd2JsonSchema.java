package com.mcsuka.xml.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mcsuka.xml.testtools.GenericTools;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestXsd2JsonSchema {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static void printJson(JsonElement o) {
        if (o != null) {
            System.out.println(GSON.toJson(o));
        } else {
            System.out.println("null");
        }
    }

    @Test
    public void testChoice() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Choice2.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Choice2.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testArray() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Array2.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Array2.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testGroup() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Group.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Group.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testComplex() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Complex.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Complex.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testSimple5() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Simple.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root5");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Simple5.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testSimple3() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Simple.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root3");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Simple3.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testWsdl() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser(
            "http://customerservice.example.com/",
            new WsdlDocumentSource("file://testdata/input/CustomerService.wsdl"));
        SchemaNode n = model.parse("updateCustomer");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/UpdateCustomer.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testRestriction() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Restriction.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Restriction.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

    @Test
    public void testRestriction1() throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser("testdata/input/Restriction.xsd", new XsdDocumentSource());
        SchemaNode n = model.parse("root2");
        JsonObject jschema = Xsd2JsonSchema.translateSchema(n);
        String expected = GenericTools.getResourceFile("testdata/output/Restriction1.json");

        Assertions.assertEquals(GSON.fromJson(expected, JsonObject.class), jschema);
    }

}
