package com.mcsuka.xml.json;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import com.mcsuka.xml.xsd.tools.XmlTools;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import com.mcsuka.xml.testtools.GenericTools;

public class TestJson2Xml {

  private static String json2xml(String rootElem, String jsonFileName, String nameSpaceUri) throws Exception {
      String jsonFile = GenericTools.getResourceFile(jsonFileName);
      Json2Xml json2xml = new Json2Xml();
      Document doc = json2xml.translate(jsonFile, rootElem, nameSpaceUri);
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
    String actual = json2xml("root2", "testdata/input/Json2XmlNoxsd1.json", null);
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsd1.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsd2() throws Exception {
    String actual = json2xml("root2", "testdata/input/Json2XmlNoxsd2.json", "urn:test");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsd2.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlStrangeCharacters() throws Exception {
    String actual = json2xml("root2", "testdata/input/Json2XmlStrangeCharacters.json", null);
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
  public void testJson2XmlXsd4() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root2", "testdata/input/Json2XmlXsd4.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd4.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd5() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root4", "testdata/input/Json2XmlXsd5.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd5.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd6() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root4", "testdata/input/Json2XmlXsd6.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd6.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd7() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root5", "testdata/input/Json2XmlXsd7.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd7.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd8() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root5", "testdata/input/Json2XmlXsd8.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd8.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd9() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root5", "testdata/input/Json2XmlXsd9.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd9.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd10() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root6", "testdata/input/Json2XmlXsd10.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd10.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd11() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root6", "testdata/input/Json2XmlXsd11.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd11.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsd12() throws Exception {
    String actual = json2xmlNs("testdata/input/Simple.xsd", "root6", "testdata/input/Json2XmlXsd12.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsd12.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsdArray() throws Exception {
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdArray.json", null);
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdArray.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsdArray2() throws Exception {
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdArray2.json", null);
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdArray2.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlNoxsdArray3() throws Exception {
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdArray3.json", null);
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdArray3.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdArray() throws Exception {
    String actual = json2xmlNs("testdata/input/Array.xsd", "root", "testdata/input/Json2XmlXsdArray.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdArray.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdArray2() throws Exception {
    String actual = json2xmlNs("testdata/input/Array2.xsd", "root", "testdata/input/Json2XmlXsdArray2.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdArray2.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdArray3() throws Exception {
    String actual = json2xmlNs("testdata/input/Array2.xsd", "root", "testdata/input/Json2XmlXsdArray3.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdArray3.xml");
    GenericTools.assertEquals(expected, actual);
  }

  @Test
  public void testJson2XmlXsdNoArray() throws Exception {
    String actual = json2xmlNs("testdata/input/NoArray.xsd", "root", "testdata/input/Json2XmlXsdNoArray.json");
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlXsdNoArray.xml");
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
    String actual = json2xml("root", "testdata/input/Json2XmlNoxsdScalar.json", null);
    String expected = GenericTools.getResourceFile("testdata/output/Json2XmlNoxsdScalar.xml");
    GenericTools.assertEquals(expected, actual);
  }

}
