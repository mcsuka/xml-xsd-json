package com.mcsuka.xml.json;

import java.util.HashMap;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaNode.DataType;
import com.mcsuka.xml.xsd.tools.XmlTools;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * JSON to XML translator, optionally with XML Schema support The XML schema is
 * used to define the root element, decide the name space of the elements,
 * whether a JSON element should be translated to an XML attribute or an XML
 * element. XML schema will also force the ordering of elements in the schema
 * sequence and creating mandatory elements. The translation is thread safe,
 * side-effect free, it may be reused in concurrent threads.
 * 
 *
 */
public class Json2Xml {

    private final SchemaNode grammar;
    private final HashMap<String, String> nsPfxMap;
    private static final Pattern GOOD_PATTERN = Pattern.compile("[a-zA-Z_][\\x2D\\x2E0-9a-zA-Z_]*");

    private static String normalizeKey(String key) {
        if (GOOD_PATTERN.matcher(key).matches()) {
            return key;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (char c : key.toCharArray()) {
            if (first) {
                first = false;
                if (Character.isLetter(c)) {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            } else {
                if (Character.isLetter(c) || Character.isDigit(c) || c == '.' || c == '-') {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Instantiate a JSON to XML translator with XSD model support.
     * 
     * @param grammar the XML Schema model
     */
    public Json2Xml(SchemaNode grammar) {
        this.grammar = grammar;
        nsPfxMap = new HashMap<>();
        getPfxMap(grammar, 0);
    }

    /**
     * Instantiate a JSON to XML translator without XSD model support.
     */
    public Json2Xml() {
        this.grammar = null;
        nsPfxMap = null;
    }

    private void getPfxMap(SchemaNode node, int idx) {
        if (!nsPfxMap.containsKey(node.getNamespace())) {
            nsPfxMap.put(node.getNamespace(), "ns" + idx + ":");
            idx++;
        }
        if (!node.isRecursive()) {
            for (SchemaNode child : node.getChildren()) {
                getPfxMap(child, idx);
            }
        }
    }

    private Element createElement(String key, Document doc) {
        key = normalizeKey(key);
        return doc.createElement(key);
    }

    private Element createElementNS(SchemaNode xsdNode, String key, Document doc) {
        Element e = xsdNode != null && xsdNode.isQualified()
                ? doc.createElementNS(xsdNode.getNamespace(), nsPfxMap.get(xsdNode.getNamespace()) + key)
                        : createElement(key, doc);
                return e;
    }

    private int walkArray(String key, JsonArray jsonArray, Element xmlNode, Document doc, SchemaNode xsdNode,
            int currLen) {
        int length = jsonArray.size();
        int maxLen = (xsdNode == null ? Integer.MAX_VALUE : xsdNode.getMaxOccurs());
        int i = 0;
        for (; i < length && currLen < maxLen; i++) {
            JsonElement arrayValue = jsonArray.get(i);
            if (arrayValue instanceof JsonArray) {
                currLen = walkArray(key, (JsonArray) arrayValue, xmlNode, doc, xsdNode, currLen);
            } else {
                Element child = null;
                if (key != null) {
                    child = createElementNS(xsdNode, key, doc);
                }
                if (arrayValue instanceof JsonObject) {
                    walkObject((JsonObject) arrayValue, child == null ? xmlNode : child, doc, xsdNode);
                } else if (arrayValue != null) {
                    if (child == null) {
                        child = createElementNS(xsdNode, "item", doc);
                    }
                    child.setTextContent(arrayValue.getAsString());
                    if (xsdNode != null && !xsdNode.isLeaf()) {
                        walkObject(new JsonObject(), child, doc, xsdNode);
                    }
                }
                if (child != null) {
                    xmlNode.appendChild(child);
                    currLen++;
                }
            }
        }
        return currLen;
    }

    private void walkObjectByXsd(JsonObject jsonNode, Element xmlNode, Document doc, SchemaNode childXsdNode, boolean optional) {
        if (childXsdNode.isIndicator()) {
            boolean optionalChild = optional || childXsdNode.getMinOccurs() == 0;
            for (SchemaNode descendantXsdNode: childXsdNode.getChildren()) {
                walkObjectByXsd(jsonNode, xmlNode, doc, descendantXsdNode, optionalChild);
                if (xmlNode.hasChildNodes()) {
                    if (childXsdNode.getIndicator() == SchemaNode.IndicatorType.choice) {
                        break;
                    } else {
                        optionalChild = false;
                    }
                }
            }
        } else if (childXsdNode.isAny()) {
            if (childXsdNode.isAttribute()) {  // any Attribute
                for (String key : jsonNode.keySet()) {
                    Attr attr = doc.createAttributeNS((childXsdNode.isQualified() ? childXsdNode.getNamespace() : null), key);
                    attr.setValue(jsonNode.getAsString());
                    xmlNode.setAttributeNodeNS(attr);
                }
            } else {  // any Element
                walkObjectNoXsd(jsonNode, xmlNode, doc);
            }
        } else {
            String key = childXsdNode.getElementName();
            JsonElement value = jsonNode.get(key);
            if (value != null) {
                if (childXsdNode.isAttribute()) {
                    Attr attr = doc.createAttributeNS((childXsdNode.isQualified() ? childXsdNode.getNamespace() : null), key);
                    if (childXsdNode.getFixedValue() != null) {
                        attr.setValue(childXsdNode.getFixedValue());
                    } else {
                        attr.setValue(value.toString());
                    }
                    xmlNode.setAttributeNodeNS(attr);
                } else {
                    if (value instanceof JsonArray) {
                        walkArray(key, (JsonArray) value, xmlNode, doc, childXsdNode, 0);
                    } else {
                        Element child = createElementNS(childXsdNode, key, doc);
                        if (value instanceof JsonObject) {
                            JsonObject jo = (JsonObject) value;
                            if (jo.size() > 0) {
                                if (childXsdNode.isSimpleType() && jo.has(Xml2Json.XML_ELEMENT_CONTENT)) {
                                    child.setTextContent(jo.get(Xml2Json.XML_ELEMENT_CONTENT).getAsString());
                                }
                                walkObject(jo, child, doc, childXsdNode);
                            }
                        } else {
                            if (childXsdNode.isSimpleType() || childXsdNode.getW3CType() == DataType.MIXED) {
                                if (childXsdNode.getFixedValue() != null) {
                                    child.setTextContent(childXsdNode.getFixedValue());
                                } else {
                                    child.setTextContent(value.getAsString());
                                }
                            }
                            if (!childXsdNode.isLeaf()) {
                                walkObject(new JsonObject(), child, doc, childXsdNode);
                            }
                        }
                        xmlNode.appendChild(child);
                    }
                }
            } else if (childXsdNode.isAttribute() && childXsdNode.getMinOccurs() > 0)  {
                Attr attr = doc.createAttributeNS((childXsdNode.isQualified() ? childXsdNode.getNamespace() : null), key);
                if (childXsdNode.getDefaultValue() != null) {
                    attr.setValue(childXsdNode.getDefaultValue());
                } else {
                    attr.setValue("");
                }
                xmlNode.setAttributeNodeNS(attr);
            } else if (!optional) {
                for (int i = 0; i < childXsdNode.getMinOccurs(); i++) {
                    Element child = createElementNS(childXsdNode, key, doc);
                    if (childXsdNode.getDefaultValue() != null) {
                        child.setTextContent(childXsdNode.getDefaultValue());
                    }
                    xmlNode.appendChild(child);
                }
            }
        }      
    }

    private void walkObjectNoXsd(JsonObject jsonNode, Element xmlNode, Document doc) {
        for (String key : jsonNode.keySet()) {
            JsonElement value = jsonNode.get(key);
            if (value instanceof JsonObject) {
                JsonObject jo = (JsonObject) value;
                Element child = createElement(key, doc);
                if (jo.size() > 0) {
                    walkObjectNoXsd(jo, child, doc);
                }
                xmlNode.appendChild(child);
            } else if (value instanceof JsonArray) {
                walkArray(key, (JsonArray) value, xmlNode, doc, null, 0);
            } else {
                Element child = createElement(key, doc);
                if (value != null) {
                    child.setTextContent(value.getAsString());
                }
                xmlNode.appendChild(child);
            }
        }
    }

    private void walkObject(JsonObject jsonNode, Element xmlNode, Document doc, SchemaNode xsdNode) {
        if (xsdNode == null || xsdNode.isAny()) {
            walkObjectNoXsd(jsonNode, xmlNode, doc);
        } else {
            for (SchemaNode childXsdNode : xsdNode.getChildren()) {
                walkObjectByXsd(jsonNode, xmlNode, doc, childXsdNode, false);
            }
        }
    }

    /**
     * Translate JSON to XML with XSD support
     * 
     * @param json JSON Object
     * @return XML representation as DOM Document
     * @throws Exception
     */
    public Document translate(JsonElement jsonRoot) {
        if (grammar == null) {
            throw new UnsupportedOperationException("Cannot execute schema-based translation, XML Schema is not initialized.");
        }
        DocumentBuilder builder = XmlTools.getDocumentBuilder();
        Document doc = builder.newDocument();
        Element xmlRoot = createElementNS(grammar, grammar.getElementName(), doc);
        doc.appendChild(xmlRoot);
        if (jsonRoot instanceof JsonObject) {
            walkObject((JsonObject)jsonRoot, xmlRoot, doc, grammar);
        } else if (jsonRoot instanceof JsonArray) {
            walkArray(null, (JsonArray) jsonRoot, xmlRoot, doc, grammar, 0);
        } else if (grammar.isSimpleType()) {    // must be a JsonPrimitive or JsonNull
            xmlRoot.setTextContent(jsonRoot.getAsString());
        } else {
            walkObject(new JsonObject(), xmlRoot, doc, grammar);    // build empty XML
        }
        return doc;
    }

    /**
     * Translate JSON to XML with XSD support
     * 
     * @param json JSON text
     * @return XML representation as DOM Document
     * @throws Exception
     */
    public Document translate(String json) throws Exception {
        JsonElement jsonRoot = (new JsonParser()).parse(json);
        Document doc = translate(jsonRoot);
        return doc;
    }

    /**
     * Translate JSON to XML without XSD
     * 
     * @param json JSON Object
     * @param rootElemName XML root element name
     * @param nameSpaceUri XML root element name space. If null, root element has
     *          no name space.
     * @return XML representation as DOM Document
     * @throws Exception
     */
    public Document translate(JsonElement jsonRoot, String rootElemName, String nameSpaceUri) {
        DocumentBuilder builder = XmlTools.getDocumentBuilder();
        Document doc = builder.newDocument();
        Element xmlRoot = (nameSpaceUri == null || nameSpaceUri.length() == 0 ? doc.createElement(rootElemName)
                : doc.createElementNS(nameSpaceUri, "pfx:" + rootElemName));
        doc.appendChild(xmlRoot);
        if (jsonRoot instanceof JsonObject) {
            walkObjectNoXsd((JsonObject)jsonRoot, xmlRoot, doc);
        } else if (jsonRoot instanceof JsonArray) {
            walkArray(null, (JsonArray) jsonRoot, xmlRoot, doc, null, 0);
        } else {    // must be a JsonPrimitive or JsonNull
            xmlRoot.setTextContent(jsonRoot.getAsString());
        }
        return doc;
    }

    /**
     * Translate JSON to XML without XSD
     * 
     * @param json JSON text
     * @param rootElemName XML root element name
     * @param nameSpaceUri XML root element name space. If null, root element has
     *          no name space.
     * @return XML representation as DOM Document
     * @throws Exception
     */
    public Document translate(String json, String rootElemName, String nameSpaceUri) throws Exception {
        JsonElement jsonRoot = (new JsonParser()).parse(json);
        Document doc = translate(jsonRoot, rootElemName, nameSpaceUri);
        return doc;
    }

}
