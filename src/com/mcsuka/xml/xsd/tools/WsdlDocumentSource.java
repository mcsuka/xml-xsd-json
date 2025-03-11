package com.mcsuka.xml.xsd.tools;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    private static String removeTnsPfx(String s, Map<String, String> prefixMap, String targetNamespace) {
        if (s.contains(":")) {
            String pfx = s.substring(0, s.indexOf(":"));
            if (targetNamespace.equals(prefixMap.get(pfx)))
                return s.substring(s.indexOf(":") + 1);
        }
        return s;
    }

    public Optional<SoapOperation> getOperation(String operationName) throws XPathExpressionException {
        XPath xp = XmlTools.newXPath();
        String soapAction = xp.evaluateExpression("//wsdl:binding/wsdl:operation[@name=" + operationName + "]/soap:operation/@soapAction",
            wsdlDoc, String.class);
        if (soapAction != null) {
            Map<String, String> pfxMap = getPrefixMap();
            String targetNamespace = xp.evaluateExpression("/wsdl:definitions/@targetNamespace",
                wsdlDoc, String.class);
            String inputMessage = xp.evaluateExpression("//wsdl:portType/wsdl:operation[@name=" +
                    operationName + "]/wsdl:input/@message", wsdlDoc, String.class);
            String outputMessage = xp.evaluateExpression("//wsdl:portType/wsdl:operation[@name=" +
                    operationName + "]/wsdl:output/@message", wsdlDoc, String.class);

            String inputElement =  xp.evaluateExpression("//wsdl:message[@name=" + removeTnsPfx(inputMessage, pfxMap, targetNamespace) +
                    "]/part/@element", wsdlDoc, String.class);
            String outputElement =  xp.evaluateExpression("//wsdl:message[@name=" + removeTnsPfx(outputMessage, pfxMap, targetNamespace) +
                    "]/part/@element", wsdlDoc, String.class);

            String inputElementPfx = inputElement.contains(":") ? inputElement.substring(0, inputElement.indexOf(":")) : null;
            String outputElementPfx = outputElement.contains(":") ? outputElement.substring(0, outputElement.indexOf(":")) : null;

            String requestRootElemName = inputElementPfx == null ? inputElement : inputElement.substring(inputElementPfx.length() + 1);
            String requestNamespace = inputElementPfx == null ? targetNamespace : pfxMap.get(inputElementPfx);
            String responseRootElemName = outputElementPfx == null ? outputElement : outputElement.substring(outputElementPfx.length() + 1);
            String responseNamespace = outputElementPfx == null ? targetNamespace : pfxMap.get(outputElementPfx);

            return Optional.of(new SoapOperation(soapAction, requestRootElemName, requestNamespace, responseRootElemName, responseRootElemName));
        }
        return Optional.empty();
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

    public record SoapOperation(
        String soapAction,
        String requestRootElementName,
        String requestNamespace,
        String responseRootElementName,
        String responseNamespace
    ) {}
}
