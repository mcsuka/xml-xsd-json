package com.mcsuka.xml.json;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaNode.DataType;
import com.mcsuka.xml.xsd.tools.XmlTools;

/**
 * XML to JSON translator, optionally with XML Schema support The XML root
 * element is only a wrapper, it is omitted from the JSON output The XML Schema
 * is used to decide whether a JSON element should be an array or to decide the
 * type of a scalar element. Please see the examples in the testdata folder. The
 * translation is thread safe, side-effect free, it may be reused in concurrent
 * threads.
 * 
 *
 */
public class Xml2Json {

    public static final String FORCE_ARRAY_ATTRIBUTE = "_jsonarray";
    public static final String FORCE_SCALAR_ATTRIBUTE = "_jsonprimitive";
    public static final String XML_ELEMENT_CONTENT = "_content";
    
    private final SchemaNode grammar;
    private final boolean ignoreAttributes;

    /**
     * Instantiate a XML to JSON translator with XSD model support.
     * 
     * @param ignoreAttributes true = skip XML attributes, false = add XML
     *          attributes to JSON, except xmlns and xsi attributes
     * @param grammar the XML Schema model
     */
    public Xml2Json(boolean ignoreAttributes, SchemaNode grammar) {
        this.grammar = grammar;
        this.ignoreAttributes = ignoreAttributes;
    }

    /**
     * Instantiate a XML to JSON translator without XSD model support.
     * 
     * @param ignoreAttributes true = skip XML attributes, false = add XML
     *          attributes to JSON, except xmlns and xsi attributes
     */
    public Xml2Json(boolean ignoreAttributes) {
        this(ignoreAttributes, null);
    }

    private static SchemaNode getDescendant(SchemaNode xsdNode, String localName) {
        if (xsdNode != null) {
            if (xsdNode.isIndicator()) {
                for (SchemaNode child: xsdNode.getChildren()) {
                    SchemaNode result = getDescendant(child, localName);
                    if (result != null) {
                        return result;
                    }
                }
            } else {
                return xsdNode.getChild(localName);
            }
        }
        return null;
    }

    private JsonArray walkRootArray(JsonArray jsonArray, Element xmlNode, SchemaNode xsdNode) {
        List<Element> nodes = XmlTools.getChildElements(xmlNode);
        if ("true".equals(XmlTools.getAttribute(xmlNode, FORCE_SCALAR_ATTRIBUTE))) {
            for (Element child : nodes) {
                jsonArray.add(child.getTextContent());
            }
        } else {
            Map<String, JsonElement> currentKeyMap = new LinkedHashMap<>();
            for (Element child : nodes) {
                String childName = child.getLocalName();
                if (currentKeyMap.containsKey(childName)) {
                    JsonObject jo = new JsonObject();
                    for (Map.Entry<String, JsonElement> entry: currentKeyMap.entrySet()) {
                        jo.add(entry.getKey(), entry.getValue());
                    }
                    jsonArray.add(jo);
                    currentKeyMap = new LinkedHashMap<>();
                }
                currentKeyMap.put(childName, walk(new JsonObject(), child, getDescendant(xsdNode, childName)));
            }
            if (currentKeyMap.size() > 0) {
                JsonObject jo = new JsonObject();
                for (Map.Entry<String, JsonElement> entry: currentKeyMap.entrySet()) {
                    jo.add(entry.getKey(), entry.getValue());
                }
                jsonArray.add(jo);
            }
        }
        return jsonArray;
    }

    private JsonElement walk(JsonObject jsonNode, Element xmlNode, SchemaNode xsdNode) {
        if (!ignoreAttributes) {
            for (Map.Entry<String, String> entry : XmlTools.getAttributes(xmlNode).entrySet()) {
                if (!entry.getKey().startsWith("xsi:") && !entry.getKey().startsWith("xmlns:")
                        && !entry.getKey().equals("xmlns") && !entry.getKey().equals(FORCE_ARRAY_ATTRIBUTE)
                        && !entry.getKey().equals(FORCE_SCALAR_ATTRIBUTE)) {
                    jsonNode.addProperty(entry.getKey(), entry.getValue());
                }
            }
        }
        List<Element> nodes = XmlTools.getChildElements(xmlNode);
        if (nodes.size() > 0) {
            for (Element child : nodes) {
                String childName = child.getLocalName();
                
                SchemaNode childXsdNode = getDescendant(xsdNode, childName);
                JsonElement value = walk(new JsonObject(), child, childXsdNode);
                JsonElement existingElem = jsonNode.get(childName);
                if (existingElem != null) {
                    if (existingElem instanceof JsonArray) {
                        ((JsonArray) existingElem).add(value);
                    } else {
                        JsonArray array = new JsonArray();
                        array.add(jsonNode.remove(childName));
                        array.add(value);
                        jsonNode.add(childName, array);
                    }
                } else {
                    // check if maxOccurs > 1 or @_jsonarray = true, create array
                    if ("true".equals(XmlTools.getAttribute(child, FORCE_ARRAY_ATTRIBUTE)) || (childXsdNode != null && childXsdNode.getMaxOccurs() > 1)) {
                        JsonArray array = new JsonArray();
                        array.add(value);
                        jsonNode.add(childName, array);
                    } else {
                        jsonNode.add(childName, value);
                    }
                }
            }
            return jsonNode;
        } else {
            String stringValue = xmlNode.getTextContent();
            JsonElement value = null;
            DataType type = (xsdNode != null ? xsdNode.getW3CType() : DataType.STRING);
            switch (type) {
            case INTEGER:
                value = new JsonPrimitive(Integer.parseInt(stringValue));
                break;
            case LONG:
                value = new JsonPrimitive(Long.parseLong(stringValue));
                break;
            case DOUBLE:
                value = new JsonPrimitive(Double.parseDouble(stringValue));
                break;
            case BOOLEAN:
                value = new JsonPrimitive(Boolean.parseBoolean(stringValue));
                break;
            case COMPLEX:
            case MIXED:
            case ANY:
                value = JsonNull.INSTANCE ;
                break;
            default:
                value = new JsonPrimitive(stringValue);
                break;
            }
            if (jsonNode.size() == 0) {
                return value;
            } else {
                jsonNode.add(XML_ELEMENT_CONTENT, value);
                return jsonNode;
            }
        }
    }

    /**
     * Translate XML to JSON with or without XSD support
     * 
     * @param xml XML text
     * @return JSONObject or JSONArray or String (Only text nodes in the XML root
     *         element)
     * @throws Exception
     */
    public JsonElement translate(String xml) throws Exception {
        DocumentBuilder builder = XmlTools.getDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));
        Element xmlRoot = doc.getDocumentElement();
        return translate(xmlRoot);
    }

    /**
     * Translate XML to JSON with or without XSD support
     * 
     * @param xmlRoot DOM document root element
     * @return JSONObject or JSONArray or String (Only text nodes in the xmlRoot)
     * @throws Exception
     */
    public JsonElement translate(Element xmlRoot) throws Exception {
        if (grammar != null && grammar.getChildren().size() == 1
                && grammar.getChildren().get(0).isIndicator()
                && grammar.getChildren().get(0).getMaxOccurs() > 1) {   // array, forced by XSD
            return walkRootArray(new JsonArray(), xmlRoot, grammar);
        } else if ("true".equals(XmlTools.getAttribute(xmlRoot, FORCE_ARRAY_ATTRIBUTE))) {   // array, forced by special attribute
            return walkRootArray(new JsonArray(), xmlRoot, grammar);
        } else { // object
            return walk(new JsonObject(), xmlRoot, grammar);
        }
    }

}
