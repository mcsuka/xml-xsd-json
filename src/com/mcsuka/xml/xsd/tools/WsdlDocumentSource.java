package com.mcsuka.xml.xsd.tools;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Load XSDs from the 'schema' sections of a single WSDL file
 */
public class WsdlDocumentSource implements DocumentSource {

    private final Document wsdlDoc;
    private Map<String, String> pfxMap = null;

    public enum OperationType {
        input, output
    }

    public WsdlDocumentSource(Document wsdlDoc) {
        this.wsdlDoc = wsdlDoc;
    }

    public WsdlDocumentSource(String wsdlUrl) throws Exception {
        this(wsdlUrl, StandardCharsets.UTF_8);
    }

    public WsdlDocumentSource(String wsdlUrl, Charset charset) throws Exception {
        if (wsdlUrl.startsWith("http://") || wsdlUrl.startsWith("https://")) {
            URL url = new URI(wsdlUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            wsdlDoc = inputStreamToDoc(conn::getInputStream, charset);
        } else {
            String fileName = wsdlUrl.startsWith("file://") ? wsdlUrl.substring(7) : wsdlUrl;
            wsdlDoc = inputStreamToDoc(() -> new FileInputStream(fileName), charset);
        }
    }

    /**
     * Get the list of soapActions from the WSDL  
     * @return list of soapAction IDs of the operations
     */
    public List<String> getSoapActions() throws XPathExpressionException {
        XPath xp = XmlTools.newXPath();
        ArrayList<String> soapActionList = new ArrayList<>();
        NodeList soapActions = (NodeList) xp.evaluate("//wsdl:binding/wsdl:operation/soap:operation/@soapAction",
                wsdlDoc, XPathConstants.NODESET);
        for (int i = 0; i < soapActions.getLength(); i++) {
            soapActionList.add(soapActions.item(i).getTextContent());
        }
        return soapActionList;
    }

    public Map<String, String> getPrefixMap() {
        if (pfxMap == null) {
            pfxMap = new HashMap<>();
            HashMap<String, String> attrs = XmlTools.getAttributes(wsdlDoc.getDocumentElement());
            for (String attrName : attrs.keySet()) {
                String attrValue = attrs.get(attrName);
                if (attrName != null) {
                    if (attrName.startsWith("xmlns:")) {
                        pfxMap.put(attrName.substring(6), attrValue);
                    }
                    if (attrName.equals("xmlns")) {
                        pfxMap.put("", attrValue);
                    }
                }
            }
        }
        return pfxMap;
    }

    /**
     * Returns the 'schema' element from the WSDL, referenced by its name space
     * URN
     * 
     * @param nsUrn name space URN of the schema inside the WSDL
     * @return the schema as a DOM Document
     */
    @Override
    public Document parse(String nsUrn) throws DocumentSourceException {
        try {
            Node sn = (Node) XmlTools.newXPath().evaluate("//xsd:schema[@targetNamespace='" + nsUrn + "']", wsdlDoc,
                    XPathConstants.NODE);
            Document schemaDoc = XmlTools.getDocumentBuilder().newDocument();
            Node isn = schemaDoc.importNode(sn, true);
            schemaDoc.appendChild(isn);
            return schemaDoc;
        } catch (Throwable t) {
            throw new DocumentSourceException("Unable to parse WSDL schema identified by urn " + nsUrn, t);
        }
    }
}
