package com.mcsuka.xml.json;

import org.junit.Test;
import org.w3c.dom.Document;

import com.mcsuka.xml.json.Json2Xml;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import com.mcsuka.xml.xsd.tools.XmlTools;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import com.mcsuka.xml.testtools.GenericTools;

public class TestJson2Xml {

  private static String json2xml(String rootElem, String jsonFileName) throws Exception {
      String jsonFile = GenericTools.getResourceFile(jsonFileName);
      Json2Xml json2xml = new Json2Xml();
      Document doc = json2xml.translate(jsonFile, rootElem, null);
      return XmlTools.renderDOM(doc);
  }

  private static String json2xmlNs(String xsdFileName, String rootElem, String jsonFileName) throws Exception {
      SchemaParser model = SchemaParserFactory.newSchemaParser(xsdFileName, new XsdDocumentSource());
      SchemaNode grammar = model.parse(rootElem);

      String jsonFile = GenericTools.getResourceFile(jsonFileName);
      Json2Xml json2xml = new Json2Xml(grammar);
      Document doc = json2xml.translate(jsonFile);
      return XmlTools.renderDOM(doc);
  }

  @Test
  public void testJson2XmlNoxsd1() throws Exception {
    String actual = json2xml("root2", "testdata/input/Json2XmlNoxsd1.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsd1.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlStrangeCharacters() throws Exception {
    String actual = json2xml("root2", "testdata/input/Json2XmlStrangeCharacters.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlStrangeCharacters.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd1() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root2", "testdata/input/Json2XmlXsd1.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd1.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd2() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root2", "testdata/input/Json2XmlXsd2.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd2.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd3() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root3", "testdata/input/Json2XmlXsd3.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd3.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsdArray() throws Exception {
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdArray.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdArray.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdArray() throws Exception {
    String actual = json2xmlNs("testdata/input/Array.xsd", "root", "testdata/input/Json2XmlXsdArray.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdArray.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdScalar() throws Exception {
    String actual = json2xmlNs("testdata/input/Scalar.xsd", "root", "testdata/input/Json2XmlXsdScalar.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdScalar.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsdScalar() throws Exception {
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdScalar.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdScalar.xml");
    GenericTools.assertEquals(expected, actual);
  }

}
