package com.mcsuka.xml.xsd.tools;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Load XSDs from the 'schema' sections of a single WSDL file
 */
public class WsdlDocumentSource implements DocumentSource {

    private static final Logger logger = LoggerFactory.getLogger(WsdlDocumentSource.class.getName());

    private final Document wsdlDoc;
    private final Map<String, SoapOperation> operationMap = new HashMap<>();
    private Map<String, String> pfxMap = null;

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
    public List<String> getSoapActions() {
        XPath xp = XmlTools.newXPath();
        ArrayList<String> soapActionList = new ArrayList<>();
        try {
            NodeList soapActions = (NodeList) xp.evaluate("//wsdl:binding/wsdl:operation/soap:operation/@soapAction",
                wsdlDoc, XPathConstants.NODESET);
            for (int i = 0; i < soapActions.getLength(); i++) {
                soapActionList.add(soapActions.item(i).getTextContent());
            }
        } catch (Exception e) {
            logger.warn("Error retrieving soap action list", e);
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

    public synchronized Optional<SoapOperation> getOperation(String operationName) {
        SoapOperation op = operationMap.get(operationName);
        if (op != null) {
            if (!NULL_OPERATION.equals(op)) {
                return Optional.of(op);
            }
        } else {
            try {
                XPath xp = XmlTools.newXPath();
                String soapAction = xp.evaluateExpression("//wsdl:binding/wsdl:operation[@name='" + operationName + "']/soap:operation/@soapAction",
                    wsdlDoc, String.class);
                if (soapAction != null) {
                    Map<String, String> pfxMap = getPrefixMap();
                    String targetNamespace = xp.evaluateExpression("/wsdl:definitions/@targetNamespace",
                        wsdlDoc, String.class);
                    String inputMessage = xp.evaluateExpression("//wsdl:portType/wsdl:operation[@name='" +
                        operationName + "']/wsdl:input/@message", wsdlDoc, String.class);
                    String outputMessage = xp.evaluateExpression("//wsdl:portType/wsdl:operation[@name='" +
                        operationName + "']/wsdl:output/@message", wsdlDoc, String.class);

                    String inputElement = xp.evaluateExpression("//wsdl:message[@name='" + removeTnsPfx(inputMessage, pfxMap, targetNamespace) +
                        "']/wsdl:part/@element", wsdlDoc, String.class);
                    String outputElement = xp.evaluateExpression("//wsdl:message[@name='" + removeTnsPfx(outputMessage, pfxMap, targetNamespace) +
                        "']/wsdl:part/@element", wsdlDoc, String.class);

                    String inputElementPfx = inputElement.contains(":") ? inputElement.substring(0, inputElement.indexOf(":")) : null;
                    String outputElementPfx = outputElement.contains(":") ? outputElement.substring(0, outputElement.indexOf(":")) : null;

                    SoapOperation newOp = new SoapOperation(soapAction,
                        inputElementPfx == null
                            ? new QName(targetNamespace, inputElement, XMLConstants.DEFAULT_NS_PREFIX)
                            : new QName(pfxMap.get(inputElementPfx), inputElement.substring(inputElementPfx.length() + 1), inputElementPfx),
                        outputElementPfx == null
                            ? new QName(targetNamespace, outputElement, XMLConstants.DEFAULT_NS_PREFIX)
                            : new QName(pfxMap.get(outputElementPfx), outputElement.substring(outputElementPfx.length() + 1), outputElementPfx));
                    operationMap.put(operationName, newOp);
                    return Optional.of(newOp);
                }
            } catch (XPathExpressionException e) {
                logger.warn("Error resolving operation " + operationName, e);
            }
            operationMap.put(operationName, NULL_OPERATION);
        }
        return Optional.empty();
    }

    public synchronized Map<String, String> getPrefixMap() {
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

    public static final SoapOperation NULL_OPERATION = new SoapOperation(
        "%%NOTFOUND%%",
        new QName("http://dummy.net", "NOTFOUNDREQUEST"),
        new QName("http://dummy.net", "NOTFOUNDRESPONSE"));

    public record SoapOperation(
        String soapAction,
        QName requestRootElement,
        QName responseRootElement
    ) {}
}
