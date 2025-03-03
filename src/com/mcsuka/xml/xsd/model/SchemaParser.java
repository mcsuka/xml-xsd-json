package com.mcsuka.xml.xsd.model;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mcsuka.xml.xsd.tools.DocumentSource;
import com.mcsuka.xml.xsd.tools.DocumentSourceException;
import com.mcsuka.xml.xsd.tools.XmlTools;

import org.w3c.dom.Document;

/**
 * The SchemaParser takes an XSD file as an input and creates a tree of SchemaNode objects.
 * XSD references are honored, includes and imports are loaded and a new SchemaParser is initialized for each import. SchemaParser
 * should not be initialized directly, but via the SchemaParserFactory. A SchemaParser instance is thread safe and side-effect free.
 */
public class SchemaParser {

    private final String logXsdLocation;
    private final boolean elementQualified;
    private final boolean attributeQualified;

    private final DocumentSource docSource;
    private final HashMap<String, String> importMap = new HashMap<>();
    private final HashMap<String, String> pfxMap = new HashMap<>();
    private final HashMap<String, Node> typeNodeMap = new HashMap<>();
    private final HashMap<String, Node> elemNodeMap = new HashMap<>();
    private final HashMap<String, Node> groupNodeMap = new HashMap<>();

    private final String targetNamespace;

    private final XPath xpath;
    private final HashMap<String, SchemaNode> modelMap = new HashMap<>();

    /**
     * Constructor should not be called directly, but via the SchemaParserFactory
     * 
     * @param xsdLocation The location of the XSD file, in the format of the DocumentSource, e.g. a URL or a file name
     * @param docSource Provides access to the XSD file. 
     */
    SchemaParser(String xsdLocation, DocumentSource docSource) throws DocumentSourceException, XPathExpressionException {
        this.docSource = docSource;
        logXsdLocation = xsdLocation;

        Document input = docSource.parse(xsdLocation);
        Element documentNode = input.getDocumentElement();

        pfxMap.put("xsd", "http://www.w3.org/2001/XMLSchema");
        pfxMap.put("xs", "http://www.w3.org/2001/XMLSchema");
        pfxMap.putAll(docSource.getPrefixMap());

        String myTargetNamespace = null;
        boolean myElementQualified = true;
        boolean myAttributeQualified = false;
        
        // get name-space prefixes
        HashMap<String, String> attrs = XmlTools.getAttributes(documentNode);
        for (String attrName : attrs.keySet()) {
            String attrValue = attrs.get(attrName);
            if (attrName != null) {
                if (attrName.startsWith("xmlns:")) {
                    pfxMap.put(attrName.substring(6), attrValue);
                }
                if (attrName.equals("xmlns")) {
                    pfxMap.put("", attrValue);
                }
                if (attrName.equals("targetNamespace")) {
                    myTargetNamespace = attrValue;
                    pfxMap.put("targetNamespace", attrValue);
                }
                if (attrName.equals("elementFormDefault") && attrValue.equals("unqualified")) {
                    myElementQualified = false;
                }
                if (attrName.equals("attributeFormDefault") && attrValue.equals("qualified")) {
                    myAttributeQualified = true;
                }
            }
        }
        if (pfxMap.get("") == null && myTargetNamespace == null) {
            myTargetNamespace = "";
            pfxMap.put("targetNamespace", myTargetNamespace);
            pfxMap.put("", myTargetNamespace);
        }
        
        targetNamespace = myTargetNamespace;
        elementQualified = myElementQualified; 
        attributeQualified = myAttributeQualified;
        xpath = XmlTools.newXPath();

        // get imports
        NodeList importNodes = (NodeList) xpath.evaluate("/xs:schema/xs:import", input, XPathConstants.NODESET);
        if (importNodes != null) {
            String xsdLocationBase = "";
            int pos = xsdLocation.lastIndexOf("/");
            if (pos >= 0) {
                xsdLocationBase = xsdLocation.substring(0, pos + 1);
            }
            for (int i = 0; i < importNodes.getLength(); i++) {
                HashMap<String, String> importAttrs = XmlTools.getAttributes(importNodes.item(i));
                String importLocation = importAttrs.get("schemaLocation");
                String ns = importAttrs.get("namespace");
                importMap.put(ns, (importLocation == null ? ns : xsdLocationBase + importLocation));
            }
        }
        getTypesAndElements(input, xsdLocation);
    }

    private Map<String, Node> mapNodes(Document xsdDoc, String path) throws XPathExpressionException {
        NodeList nodes = (NodeList) xpath.evaluate(path, xsdDoc, XPathConstants.NODESET);
        Map<String, Node> map = new HashMap<>();
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                map.put(XmlTools.getAttribute(nodes.item(i), "name"), nodes.item(i));
            }
        }
        return map;
    }

    private void getTypesAndElements(Document myInput, String xsdLocation) throws XPathExpressionException, DocumentSourceException {
        typeNodeMap.putAll(mapNodes(myInput, "/xs:schema/xs:complexType"));
        typeNodeMap.putAll(mapNodes(myInput, "/xs:schema/xs:simpleType"));
        elemNodeMap.putAll(mapNodes(myInput, "/xs:schema/xs:element"));
        groupNodeMap.putAll(mapNodes(myInput, "/xs:schema/xs:group"));

        NodeList includeNodes = (NodeList) xpath.evaluate("/xs:schema/xs:include", myInput, XPathConstants.NODESET);
        if (includeNodes != null) {
            String xsdLocationBase = "";
            int pos = xsdLocation.lastIndexOf("/");
            if (pos >= 0) {
                xsdLocationBase = xsdLocation.substring(0, pos + 1);
            }
            for (int i = 0; i < includeNodes.getLength(); i++) {
                HashMap<String, String> importAttrs = XmlTools.getAttributes(includeNodes.item(i));
                String includeLocation = importAttrs.get("schemaLocation");
                Document input = docSource.parse(SchemaParserFactory.normalizeUrl(xsdLocationBase + includeLocation));
                getTypesAndElements(input, xsdLocationBase + includeLocation);
            }
        }
    }

    /**
     * Parse an XSD (or a set of related XSDs), starting from the root element.
     * @param rootElemName The localName of the root of the schema tree. Either an explicitly defined element
     * (e.g. "root") or a path of localNames to an element (e.g. root/body/result)
     */
    public SchemaNode parse(String rootElemName) throws XPathExpressionException, DocumentSourceException {
        if (rootElemName.startsWith("/")) {
            rootElemName = rootElemName.substring(1);
        }
        SchemaNode rootElemNode = modelMap.get(rootElemName);
        if (rootElemNode == null) {
            if (rootElemName.contains("/")) {
                String[] tokens = rootElemName.split("/");
                SchemaNode currentModelNode = parseGlobalElement(tokens[0], null);
                for (int i = 1; i < tokens.length; i++) {
                    if (currentModelNode != null) {
                        currentModelNode =  currentModelNode.getChild(tokens[i]);
                    } else {
                        break;
                    }
                }
                if (currentModelNode != null) {
                    currentModelNode.setAsRoot();
                    rootElemNode = currentModelNode;
                }
            } else {
                rootElemNode = parseGlobalElement(rootElemName, null);
            }
            if (rootElemNode == null) {
                throw new IllegalArgumentException("Could not find element '" + rootElemName + "' in XSD " + logXsdLocation);
            }
            modelMap.put(rootElemName, rootElemNode);
        }
        return rootElemNode;
    }

    private SchemaNode parseGlobalElement(String localName, SchemaNode parentModelNode) throws XPathExpressionException, DocumentSourceException {
        SchemaNode thisModelNode = null;
        if (elemNodeMap.containsKey(localName)) {
            thisModelNode = parseElement(elemNodeMap.get(localName), parentModelNode, true);
        } else if (groupNodeMap.containsKey(localName)) {
            thisModelNode = parentModelNode;
            appendChildNodes(groupNodeMap.get(localName), thisModelNode);
        }
        return thisModelNode;
    }

    private SchemaNode parseElement(Node xsdNode, SchemaNode parentModelNode, boolean global) throws XPathExpressionException, DocumentSourceException {
        if (xsdNode instanceof Element) {
            HashMap<String, String> attrs = XmlTools.getAttributes(xsdNode);
            if (attrs.containsKey("name")) {             // element definition
                String name = attrs.get("name");
                String fixedValue = attrs.get("fixed");
                String defaultValue = (fixedValue == null ? attrs.get("default") : fixedValue);
                String form = attrs.get("form");
                boolean qualified = (form == null ? elementQualified || global : "qualified".equals(form));
                SchemaNode thisModelNode = new SchemaNode(name, null, targetNamespace,
                        qualified, false, false, defaultValue, fixedValue,
                        formatCardinality(attrs, "minOccurs"), formatCardinality(attrs, "maxOccurs"));
                if (parentModelNode != null) {
                    parentModelNode.addChild(thisModelNode);
                }
                if (attrs.containsKey("type")) {
                    applyType(attrs.get("type"), thisModelNode);
                } else if (!thisModelNode.isRecursive()) {
                    appendChildNodes(xsdNode, thisModelNode);
                }
                thisModelNode.setDocumentation(getDocumentation(xsdNode));
                return thisModelNode;
            } else if (attrs.containsKey("ref")) {      // element reference
                String elemRef = attrs.get("ref");
                int pos = elemRef.indexOf(":");
                String pfx = (pos < 0 ? "" : elemRef.substring(0, pos));
                String ns = pfxMap.get(pfx);
                String localName = (pos < 0 ? elemRef : elemRef.substring(pos + 1));
                return ns.equals(targetNamespace)
                    ? parseGlobalElement(localName, parentModelNode)
                    : SchemaParserFactory
                        .newSchemaParser(importMap.get(ns), docSource)
                        .parseGlobalElement(localName, parentModelNode);
            }
        }
        return null;
    }

    private void appendChildNodes(Node xsdNode, SchemaNode modelNode) throws XPathExpressionException, DocumentSourceException {
        // get children of the element

        ArrayList<Element> children = XmlTools.getChildElements(xsdNode);
        if (!children.isEmpty()) {
            for (Element child : children) {
                String childName = child.getLocalName();
                HashMap<String, String> attrs = XmlTools.getAttributes(child);

                if ("restriction".equals(childName)) {
                    applyType(attrs.get("base"), modelNode);
                    ArrayList<Element> restrictionList = XmlTools.getChildElements(child);
                    String prevNodeName = null;
                    for (Element element: restrictionList) {
                        String nodeName = element.getLocalName();
                        if (!nodeName.equals(prevNodeName)) {
                            if (modelNode.getRestrictions() != null) {
                                modelNode.appendRestrictions(", ");
                            }
                            modelNode.appendRestrictions(nodeName + ":");
                        } 
                        if ("pattern".equals(nodeName) || "enumeration".equals(nodeName)) {
                            modelNode.appendRestrictions(" '" + XmlTools.getAttribute(element, "value") + "'");
                        } else {
                            modelNode.appendRestrictions(XmlTools.getAttribute(element, "value"));
                        }
                        prevNodeName = nodeName;
                    }
                } else if ("extension".equals(childName)) {
                    applyType(attrs.get("base"), modelNode);
                    appendChildNodes(child, modelNode);
                } else if ("attribute".equals(childName)) {
                    appendAttribute(child, modelNode);
                } else if ("sequence".equals(childName) || "choice".equals(childName) || "all".equals(childName)) {
                    appendIndicator(child, modelNode);
                } else if ("simpleType".equals(childName) || "simpleContent".equals(childName)) {
                    appendChildNodes(child, modelNode);
                } else if ("complexType".equals(childName) || "complexContent".equals(childName)) {
                    appendChildNodes(child, modelNode);
                    if (attrs.containsKey("mixed") && "true".equals(attrs.get("mixed"))) {
                        modelNode.setMixed();
                    }
                } else if ("anyAttribute".equals(childName)) {
                    modelNode.addChild(new SchemaNode("*", targetNamespace, attributeQualified, true, true, 0, Integer.MAX_VALUE));
                }
            }
        }
    }

    private void appendIndicator(Element indicatorXsdNode, SchemaNode modelNode) throws XPathExpressionException, DocumentSourceException {

        // check for attributes and documentation
        HashMap<String, String> attrs = XmlTools.getAttributes(indicatorXsdNode);
        SchemaNode indicatorNode = new SchemaNode(SchemaNode.IndicatorType.valueOf(indicatorXsdNode.getLocalName()), targetNamespace,
                formatCardinality(attrs, "minOccurs"), formatCardinality(attrs, "maxOccurs"));
        modelNode.addChild(indicatorNode);

        // get children of indicator node
        ArrayList<Element> children = XmlTools.getChildElements(indicatorXsdNode);
        for (Element child : children) {
            String childName = child.getLocalName();
            if ("group".equals(childName) || "element".equals(childName)) {
                parseElement(child, indicatorNode, false);
            } else if ("sequence".equals(childName) || "choice".equals(childName) || "all".equals(childName)) {
                appendIndicator(child, indicatorNode);
            } else if ("any".equals(childName)) {
                HashMap<String, String> childAttrs = XmlTools.getAttributes(child);
                indicatorNode.addChild(new SchemaNode("*", targetNamespace, false, false, true,
                        formatCardinality(childAttrs, "minOccurs"), formatCardinality(childAttrs, "maxOccurs")));
            }
        }
    }

    private void appendAttribute(Node xsdNode, SchemaNode parentModelNode) throws XPathExpressionException, DocumentSourceException {
        if (xsdNode instanceof Element) {
            HashMap<String, String> attrs = XmlTools.getAttributes(xsdNode);
            if (attrs.containsKey("name")) {
                String name = attrs.get("name");
                String use = attrs.get("use");
                int mio = ("required".equals(use) ? 1 : 0);
                int mao = ("prohibited".equals(use) ? 0 : 1);
                String defaultValue = null;
                String fixedValue = null;
                String restrictions = null;
                if (attrs.containsKey("default")) {
                    defaultValue = attrs.get("default");
                    restrictions = "default value: " + defaultValue;
                }
                if (attrs.containsKey("fixed")) {
                    defaultValue = attrs.get("fixed");
                    fixedValue = defaultValue;
                    restrictions = "fixed value: " + defaultValue;
                }
                SchemaNode attributeModelNode = new SchemaNode(name, null, targetNamespace, attributeQualified, true, false,
                        defaultValue, fixedValue, mio, mao);
                attributeModelNode.setRestrictions(restrictions);
                parentModelNode.addChild(attributeModelNode);
                appendChildNodes(xsdNode, attributeModelNode);
            }
        }
    }

    private void applyLocalType(String localName, SchemaNode thisModelNode) throws XPathExpressionException, DocumentSourceException {
        Node typeXsdNode = typeNodeMap.get(localName);
        thisModelNode.setCustomType(localName);
        thisModelNode.setDocumentation(getDocumentation(typeXsdNode));
        if (!thisModelNode.isRecursive()) {
            appendChildNodes(typeXsdNode, thisModelNode);
        }
    }

    private void applyType(String typeName, SchemaNode thisModelNode) throws XPathExpressionException, DocumentSourceException {
        int pos = typeName.indexOf(":");
        String pfx = (pos < 0 ? "" : typeName.substring(0, pos));
        String ns = pfxMap.get(pfx);
        String localName = (pos < 0 ? typeName : typeName.substring(pos + 1));
        if ("http://www.w3.org/2001/XMLSchema".equals(ns)) {
            thisModelNode.setW3CType(localName);
        } else {
            if (ns.equals(targetNamespace)) {
                applyLocalType(localName, thisModelNode);
            } else {
                SchemaParser doc = SchemaParserFactory.newSchemaParser(importMap.get(ns), docSource);
                doc.applyLocalType(localName, thisModelNode);
            }
        }
    }

    private static String getDocumentation(Node xsdNode) {
        String documentation = null;
        try {
            for (Element e: XmlTools.getChildElements(xsdNode)) {
                if ("annotation".equals(e.getLocalName())) {
                    for (Element e2: XmlTools.getChildElements(e)) {
                        if ("documentation".equals(e2.getLocalName())) {
                            documentation = e2.getTextContent();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // do nothin'
        }
        return documentation;
    }

    private static int formatCardinality(Map<String,String> attributes, String attributeName) {
        if (attributes == null) {
            return 1;
        }
        String xOccurs = attributes.get(attributeName);
        if (xOccurs == null) {
            return 1;
        }
        if (xOccurs.equals("unbounded")) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(xOccurs);
    }

}