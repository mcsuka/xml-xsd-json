package com.mcsuka.xml.json;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import com.mcsuka.xml.testtools.GenericTools;

public class TestXml2Json {

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

    private static String xml2json(String xmlFileName, boolean ignoreAttributes) throws Exception {
        String xmlFile = GenericTools.getResourceFile(xmlFileName);
        Xml2Json xml2json = new Xml2Json(ignoreAttributes);
        JsonElement o = xml2json.translate(xmlFile);
        return json2String(o);
    }

    private static String xml2jsonNs(String xsdFileName, String rootElem, String xmlFileName, boolean ignoreAttributes) throws Exception {
        String xmlFile = GenericTools.getResourceFile(xmlFileName);
        SchemaParser model = SchemaParserFactory.newSchemaParser(xsdFileName, new XsdDocumentSource());
        SchemaNode grammar = model.parse(rootElem);

        Xml2Json xml2json = new Xml2Json(ignoreAttributes, grammar);
        JsonElement o = xml2json.translate(xmlFile);
        return json2String(o);
    }

    @Test
    public void testXml2JsonNoxsdNoattribute() throws Exception {
        String actual = xml2json("testdata/input/Xml2JsonNoxsdNoattribute.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonNoxsdNoattribute.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonXsdNoattribute() throws Exception {
        String actual = xml2jsonNs("testdata/input/Simple.xsd", "root2", "testdata/input/Xml2JsonXsdNoattribute.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonXsdNoattribute.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonXsdAttribute() throws Exception {
        String actual = xml2jsonNs("testdata/input/Simple.xsd", "root3", "testdata/input/Xml2JsonXsdAttribute.xml", false);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonXsdAttribute.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonXsdAttribute2() throws Exception {
        String actual = xml2jsonNs("testdata/input/Simple.xsd", "root2", "testdata/input/Xml2JsonXsdAttribute2.xml", false);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonXsdAttribute2.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonXsdArray() throws Exception {
        String actual = xml2jsonNs("testdata/input/Array.xsd", "root", "testdata/input/Xml2JsonXsdArray.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonXsdArray.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonNoxsdArray() throws Exception {
        String actual = xml2json("testdata/input/Xml2JsonNoxsdArray.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonNoxsdArray.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonNoxsdScalarArray() throws Exception {
        String actual = xml2json("testdata/input/Xml2JsonNoxsdScalarArray.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonNoxsdScalarArray.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonNoxsdChildArray() throws Exception {
        String actual = xml2json("testdata/input/Xml2JsonNoxsdChildArray.xml", true);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonNoxsdChildArray.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonXsdScalar() throws Exception {
        String actual = xml2jsonNs("testdata/input/Scalar.xsd", "root", "testdata/input/Xml2JsonNoxsdScalar.xml", false);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonXsdScalar.json");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testXml2JsonNoxsdScalar() throws Exception {
        String actual = xml2json("testdata/input/Xml2JsonNoxsdScalar.xml", false);
        String expected = GenericTools.getResourceFile("testdata/output/Xml2JsonNoxsdScalar.json");
        GenericTools.assertEquals(expected, actual);
    }
}
