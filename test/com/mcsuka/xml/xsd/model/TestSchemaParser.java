package com.mcsuka.xml.xsd.model;

import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import org.junit.jupiter.api.Test;

import com.mcsuka.xml.xsd.tools.DocumentSourceException;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;
import com.mcsuka.xml.xsd.tools.XsdDocumentSource;
import com.mcsuka.xml.testtools.GenericTools;

public class TestSchemaParser {

    private static String collectResult(SchemaNode n) {
        StringBuilder sb = new StringBuilder();
        collectResult(n, sb);
        return sb.toString();
    }
    
    private static void collectResult(SchemaNode n, StringBuilder s) {
        s.append(n).append("\n");
        if (!n.isRecursive()) {
            for (SchemaNode c : n.getChildren()) {
                collectResult(c, s);
            }
        }
    }

    private static String parseXsdFile(String file, String root) throws XPathExpressionException, DocumentSourceException {
        SchemaParser model = SchemaParserFactory.newSchemaParser(file, new XsdDocumentSource());
        SchemaNode n = model.parse(root);
        return collectResult(n);
    }

    private static String parseWsdlUrl(String wsdlUrl, String nsUrl, String root) throws Exception {
        SchemaParser model = SchemaParserFactory.newSchemaParser(nsUrl, new WsdlDocumentSource(wsdlUrl));
        SchemaNode n = model.parse(root);
        return collectResult(n);
    }

    @Test
    public void testSimpleRoot1() throws Exception {
        String actual = parseXsdFile("testdata/input/Simple.xsd", "root1");
        String expected = GenericTools.getResourceFile("testdata/output/SimpleRoot1.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleRoot2() throws Exception {
        String actual = parseXsdFile("testdata/input/Simple.xsd", "root2");
        String expected = GenericTools.getResourceFile("testdata/output/SimpleRoot2.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleRoot2Address() throws Exception {
        String actual = parseXsdFile("testdata/input/Simple.xsd", "root2/address");
        String expected = GenericTools.getResourceFile("testdata/output/SimpleRoot2Address.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testSimpleRoot3() throws Exception {
        String actual = parseXsdFile("testdata/input/Simple.xsd", "root3");
        String expected = GenericTools.getResourceFile("testdata/output/SimpleRoot3.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testComplex() throws Exception {
        String actual = parseXsdFile("testdata/input/Complex.xsd", "root");
        String expected = GenericTools.getResourceFile("testdata/output/Complex.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testRestriction() throws Exception {
        String actual = parseXsdFile("testdata/input/Restriction.xsd", "root");
        String expected = GenericTools.getResourceFile("testdata/output/Restriction.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testGroup() throws Exception {
        String actual = parseXsdFile("testdata/input/Group.xsd", "root");
        String expected = GenericTools.getResourceFile("testdata/output/Group.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testWsdl() throws Exception {
        String actual = parseWsdlUrl("file://testdata/input/OneService.wsdl",
                "http://dummy.net/RequestMessage.xsd", "RequestMessage");
        String expected = GenericTools.getResourceFile("testdata/output/OneServiceWSDL.txt");
        GenericTools.assertEquals(expected, actual);
    }

    @Test
    public void testSoapActions() throws Exception {
        WsdlDocumentSource ws = new WsdlDocumentSource("testdata/input/OneService.wsdl");
        List<String> actual = ws.getSoapActions();
        GenericTools.assertEquals("Operation", actual.get(0));
    }

}
