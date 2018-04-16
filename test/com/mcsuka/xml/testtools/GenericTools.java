package com.mcsuka.xml.testtools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Assert;

public class GenericTools {


    private static final String REMOVE_NAMESPACES_XSLT = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
        + "   <xsl:output method=\"xml\" version=\"1.0\" encoding=\"UTF-8\" indent=\"yes\"/>"
        + "   <xsl:template match=\"comment()\">" 
        + "      <xsl:copy>"
        + "         <xsl:apply-templates/>" 
        + "      </xsl:copy>" 
        + "   </xsl:template>" 
        + "   <xsl:template match=\"*\">"
        + "      <xsl:element name=\"{local-name()}\">" 
        + "         <xsl:for-each select=\"@*\">"
        + "            <xsl:attribute name=\"{local-name()}\">"
        + "            </xsl:attribute>" 
        + "         </xsl:for-each>" 
        + "         <xsl:apply-templates/>"
        + "      </xsl:element>" 
        + "   </xsl:template>" 
        + "</xsl:stylesheet>";

    public static String removeNamespaces(String xml) throws TransformerException {
      Transformer transformer = TransformerFactory
          .newInstance("net.sf.saxon.TransformerFactoryImpl", ClassLoader.getSystemClassLoader())
          .newTransformer(new StreamSource(new StringReader(REMOVE_NAMESPACES_XSLT)));
      StringWriter sw = new StringWriter();
      transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(sw));
      return sw.toString();
    }

    public static String getResourceFile(String fileName) throws IOException {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(new FileInputStream(fileName), Charset.forName("utf-8")));
        StringBuilder sb = new StringBuilder();
        String l = br.readLine();
        while (l != null) {
          sb.append(l);
          l = br.readLine();
          if (l != null) {
            sb.append("\n");
          }
        }
        br.close();
        return sb.toString();
      }

    public static void assertEquals(String expected, String actual) {
        if (expected != null && actual != null) {
          expected = expected.trim();
          actual = actual.trim();
        }
        Assert.assertEquals(expected, actual);
      }


}
