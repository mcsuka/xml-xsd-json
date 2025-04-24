package com.mcsuka.xml.xsd.tools;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Simple tools to handle XML documents 
 *
 */
public class XmlTools {

    private static final String docBuilderFactoryClass = "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl";

    private static final SimpleNamespaceContext simpleNamespaceCtx = new SimpleNamespaceContext();

    private static final ThreadLocal<DocumentBuilderFactory> localDocumentBuilderFactory = new ThreadLocal<>() {

        @Override
        public DocumentBuilderFactory initialValue() {
            try {
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance(docBuilderFactoryClass,
                        ClassLoader.getSystemClassLoader());
                domFactory.setNamespaceAware(true);
                return domFactory;
            } catch (Exception e) {
                throw new RuntimeException("Could not create DocumentBuilderFactory", e);
            }
        }

        @Override
        public DocumentBuilderFactory get() {
            return super.get();
        }
    };

    private static final ThreadLocal<DocumentBuilder> localDocumentBuilder = new ThreadLocal<>() {

        @Override
        public DocumentBuilder initialValue() {
            try {
                DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance(docBuilderFactoryClass,
                        ClassLoader.getSystemClassLoader());
                domFactory.setNamespaceAware(true);
                return domFactory.newDocumentBuilder();
            } catch (Exception e) {
                throw new RuntimeException("Could not create DocumentBuilder", e);
            }
        }

        @Override
        public DocumentBuilder get() {
            DocumentBuilder builder = super.get();
            builder.reset();
            return builder;
        }
    };

    private static final ThreadLocal<XPathFactory> localXPathFactory = new ThreadLocal<>() {

        @Override
        public XPathFactory initialValue() {
            try {
                String xpathFactoryClass = "net.sf.saxon.xpath.XPathFactoryImpl";
                String xpathFactoryUri = XPathFactory.DEFAULT_OBJECT_MODEL_URI;
                return XPathFactory.newInstance(xpathFactoryUri, xpathFactoryClass,
                        ClassLoader.getSystemClassLoader());
            } catch (Exception e) {
                throw new RuntimeException("Could not create XPathFactory", e);
            }
        }

        @Override
        public XPathFactory get() {
            return super.get();
        }
    };

    private static final ThreadLocal<Transformer> localTransformer = new ThreadLocal<>() {

        @Override
        public Transformer initialValue() {
            try {
                String transformerFactoryClass = "net.sf.saxon.TransformerFactoryImpl";
                Transformer transformer = TransformerFactory
                        .newInstance(transformerFactoryClass, ClassLoader.getSystemClassLoader()).newTransformer();
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                return transformer;
            } catch (Exception e) {
                throw new RuntimeException("Could not create Transformer", e);
            }
        }

        @Override
        public Transformer get() {
            return super.get();
        }
    };

    public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        return localDocumentBuilderFactory.get().newDocumentBuilder();
    }

    public static DocumentBuilder getDocumentBuilder() {
        return localDocumentBuilder.get();
    }

    public static XPathFactory getXPathFactory() {
        return localXPathFactory.get();
    }

    private static Transformer getTransformer() {
        return localTransformer.get();
    }

    public static XPath newXPath() {
        XPath x = getXPathFactory().newXPath();
        x.setNamespaceContext(simpleNamespaceCtx);
        return x;
    }

    public static String renderDOM(Document doc) throws TransformerException {
        return renderDOM(doc, true);
    }

    public static String renderDOM(Document doc, boolean prettyPrint) throws TransformerException {
        Transformer transformer = getTransformer();
        if (prettyPrint) {
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } else {
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
        }
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    public static String formatXML(String xml) throws TransformerException {
        StringWriter sw = new StringWriter();
        Transformer transformer = getTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(sw));

        return sw.toString();
    }

    public static Document parseXMLFile(File file) throws SAXException, IOException {
        DocumentBuilder domBuilder = getDocumentBuilder();
        FileReader fileReader = new FileReader(file);
        InputSource inputSource = new InputSource(fileReader);
        Document doc = domBuilder.parse(inputSource);
        fileReader.close();
        return doc;
    }

//    public static Document parseXML(String xml) throws SAXException, IOException {
//        DocumentBuilder domBuilder = getDocumentBuilder();
//        InputSource inputSource = new InputSource(new StringReader(xml));
//        Document doc = domBuilder.parse(inputSource);
//        return doc;
//    }

    @NotNull
    public static ArrayList<Element> getChildElements(@NotNull Node n) {
        ArrayList<Element> result = new ArrayList<>();

        NodeList children = n.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node child = children.item(j);
            if (child instanceof Element) {
                result.add((Element) child);
            }
        }

        return result;
    }

//    public static Node getChildNode(String localName, @NotNull Node node) {
//        NodeList children = node.getChildNodes();
//        for (int i = 0; i < children.getLength(); i++) {
//            Node child = children.item(i);
//            if (localName.equals(child.getLocalName())) {
//                return child;
//            }
//        }
//        return null;
//    }

    @NotNull
    public static HashMap<String, String> getAttributes(@NotNull Node n) {
        HashMap<String, String> result = new HashMap<>();
        NamedNodeMap attrs = n.getAttributes();
        if (attrs != null) {
            for (int i = 0; i < attrs.getLength(); i++) {
                result.put(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
            }
        }
        return result;
    }

    public static String getAttribute(@NotNull Node n, String name) {
        NamedNodeMap attrs = n.getAttributes();
        if (attrs != null) {
            Node value = attrs.getNamedItem(name);
            if (value != null) {
                return value.getNodeValue();
            }
        }
        return null;
    }

    public static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * Extended namespace context for XPATH queries
     */
    public static class SimpleNamespaceContext implements javax.xml.namespace.NamespaceContext {

        public String getNamespaceURI(String prefix) {
            return switch (prefix) {
                case null        -> throw new IllegalArgumentException("Null prefix");
                case "xml"       -> XMLConstants.XML_NS_URI;
                case "xmlns"     -> XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                case "xsd", "xs" -> XMLConstants.W3C_XML_SCHEMA_NS_URI;
                case "wsdl"      -> "http://schemas.xmlsoap.org/wsdl/";
                case "soap"      -> "http://schemas.xmlsoap.org/wsdl/soap/";
                case "soapenv",
                     "SOAP-ENV"  -> SOAP_ENVELOPE_NS;
                default          -> "";
            };
        }

        /**
         * Not implemented.
         * This method isn't necessary for XPath processing.
         */
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not implemented.
         * This method isn't necessary for XPath processing.
         */
        public Iterator<String> getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }

    }

}