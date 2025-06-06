package com.mcsuka.xml.xsd.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A tree of SchemaNodes provides a simplified view of an XmlSchema. Each SchemaNode represents one of the following:
 * <ul>
 * <li>an element: xsd:element or xsd:any</li>
 * <li>an attribute: xsd:attribute or xsd:anyAttribute</li>
 * <li>an indicator: xsd:sequence xsd:all or xsd:choice</li>
 * </ul>
 * For the information stored about the node, please refer to the constructors and the getter methods. 
 * <br/>
 * Each SchemaNode maintains a list of its children, except for recursive element definitions. Recursive elements 
 * (the 2nd occurrence with the same complex type) will have a reference to the 1st occurrence.   
 */
public class SchemaNode {

    /**
     * The identified 'w3c' data types of elements or attributes. All simple or complex types are mapped to one of these.
     */
    public enum DataType {
        STRING, INTEGER, LONG, DOUBLE, BOOLEAN, DATE, DATETIME, TIME, COMPLEX, MIXED, ANY
    }

    /**
     * Types of XSD indicators
     */
    public enum IndicatorType {
        sequence, all, choice
    }

    public final static int UNBOUNDED_VALUE = Integer.MAX_VALUE;

    private final static Map<String, String> immutableEmptyMap = Collections.emptyMap();
    
    private final String elementName;
    private final IndicatorType indicator;
    private final int minOccurs;
    private final int maxOccurs;
    private final boolean any;
    private final boolean attribute;
    private final boolean qualified;
    private final String defaultValue;
    private String unknownValue;
    private final String fixedValue;
    private final String namespace;
    private String documentation;
    private Map<String,String> restrictions;
    private String customType;
    private DataType w3cType;
    private String path;
    private SchemaNode recursiveParent;

    private SchemaNode parent;
    private final HashMap<String, SchemaNode> childMap = new HashMap<>();
    private final List<SchemaNode> children = new ArrayList<>();

    /**
     * Create an indicator SchemaNode
     * 
     * @param indicator use the correct IndicatorType for indicator nodes, null otherwise
     * @param namespace target name space of the XSD 
     * @param minOccurs minimum occurrence of the indicator
     * @param maxOccurs maximum occurrence of the indicator. Integer.MAX_VALUE is used to define "unbounded"
     */
    SchemaNode(IndicatorType indicator, String namespace, int minOccurs, int maxOccurs) {
        this("", indicator, namespace, false, false, false, null, null, minOccurs, maxOccurs);
    }

    /**
     * Create an element or attribute SchemaNode
     * 
     * @param elementName localName of the element or attribute. For indicator and any nodes it is overwritten with values "" and "*" respectively
     * @param namespace name space of the element or attribute 
     * @param qualified whether the element or attribute should be placed in its name space
     * @param attribute whether it is an attribute
     * @param any whether it is an "any" type
     * @param minOccurs minimum occurrence of the element or attribute
     * @param maxOccurs maximum occurrence of the element or attribute. Integer.MAX_VALUE is used to define "unbounded"
     */
    SchemaNode(String elementName, String namespace, boolean qualified, boolean attribute, boolean any, int minOccurs,
            int maxOccurs) {
        this(elementName, null, namespace, qualified, attribute, any, null, null, minOccurs, maxOccurs);
    }

    /**
     * Create a SchemaNode
     * 
     * @param elementName localName of the element or attribute. For indicator and any nodes it is overwritten with values "" and "*" respectively
     * @param indicator use the correct IndicatorType for indicator nodes, null otherwise
     * @param namespace name space of the element or attribute 
     * @param qualified whether the element or attribute should be placed in its name space
     * @param attribute whether it is an attribute
     * @param any whether it is an "any" type
     * @param defaultValue default value of the element or attribute, null if there is no default value. 
     * Please note, if no default value is defined then a dummy default value is assigned to every simple type,
     * to allow auto-generation of the XML element
     * @param fixedValue fixed value of the element or attribute, null if there is no default value
     * @param minOccurs minimum occurrence of the element, attribute or indicator
     * @param maxOccurs maximum occurrence of the element, attribute or indicator. Integer.MAX_VALUE is used to define "unbounded"
     */
    SchemaNode(String elementName, IndicatorType indicator, String namespace, boolean qualified, boolean attribute, 
            boolean any, String defaultValue, String fixedValue, int minOccurs, int maxOccurs) {
        if (any) {
            this.elementName = "*";
        } else if (indicator != null) {
            this.elementName = "";
        } else {
            this.elementName = elementName;
        }
        this.indicator = indicator;
        this.minOccurs = minOccurs;
        this.maxOccurs = maxOccurs;
        this.path = "".equals(elementName) ? "" :  "/" + elementName; // implicit root, until attached to parent
        this.any = any;
        this.attribute = attribute;
        this.qualified = qualified;
        this.defaultValue = defaultValue;
        this.fixedValue = fixedValue;
        this.unknownValue = "";
        this.namespace = namespace;
        this.customType = null;
        this.w3cType = (any ? DataType.ANY :DataType.STRING);
        this.recursiveParent = null;
    }

    public String toString() {
        if (indicator != null) {
            return "XmlSchemaNode {" + path + "/, " + indicator + ", " + minOccurs + ".." + maxOccurs + "}";
        } else {
            return "XmlSchemaNode {" + path + ", " + w3cType + ", " + minOccurs + ".." + maxOccurs
                    + (customType != null ? ", TYPE:" + customType : "") + ", NS:" + namespace
                    + (qualified ? ", QUALIFIED" : ", UNQUALIFIED") + (attribute ? ", ATTR" : "") + (any ? ", ANY" : "")
                    + ", \"" + restrictionsToText() + "\""
                    + ", \"" + documentation + "\"" + "}";
        }
    }

    /**
     * Convenience method to draw the XSD tree under this SchemaNode as a text document. One line printed per SchemaNode.
     */
    public String dumpTree() {
        return dumpTree("");
    }
    
    private String dumpTree(String prefix) {
        String childPrefix = prefix + "  ";
        StringBuilder sb = new StringBuilder();
        if(isIndicator()) {
            sb.append(prefix)
                .append("[ ")
                .append(indicator)
                .append(" ")
                .append(getCardinality())
                .append("\n");
        } else {
            sb.append(prefix)
                .append(attribute ? "@" + elementName : elementName)
                .append(' ').append(getCardinality())
                .append(' ').append(w3cType)
                .append(' ').append(documentation == null ? "" : documentation)
                .append(' ').append(restrictions == null ? "" : restrictionsToText())
                .append(' ').append(fixedValue == null ? "" : "fixedValue=" + fixedValue)
                .append(' ').append(defaultValue == null ? "" : "defaultValue=" + defaultValue)
                .append('\n');
        }
        if (recursiveParent == null) {
            for (SchemaNode child: children) {
                sb.append(child.dumpTree(childPrefix));
            }
        } else {
            sb.append(childPrefix)
                .append("... recursive definition, children omitted\n");
        }
        if(isIndicator()) {
            sb.append(prefix)
                .append("]\n");
        }        
        return sb.toString();
    }

    void setAsRoot() {
        this.path = "/" + elementName;
        for (SchemaNode child : children) {
            child.updatePath(this.path);
        }
    }

    void addChild(SchemaNode child) {
        children.add(child);
        if (!child.isIndicator()) {
            // it is invalid to have 2 children with the same local name!
            childMap.put(child.getElementName(), child);
        }
        child.parent = this;
        child.updatePath(path);
        child.checkRecursion();
        if (w3cType != DataType.COMPLEX && w3cType != DataType.MIXED && !child.isAttribute()) {
            w3cType = DataType.COMPLEX;
        }
    }

    public String getElementName() {
        return elementName;
    }

    private String getGlobalDefinition() {
        return (customType == null ? elementName + "@" : customType + "#") + namespace;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public SchemaNode getParent() {
        return parent;
    }

    public List<SchemaNode> getChildren() {
        if (recursiveParent != null) {
            return recursiveParent.children;
        } else {
            return children;
        }
    }
    
    public SchemaNode getChild(String childName) {
        if (recursiveParent != null) {
            return recursiveParent.getChild(childName);
        } else {
            if (childMap.containsKey(childName)) {
                return childMap.get(childName);
            } else {
                for (SchemaNode child: children) {
                    if (child.isIndicator()) {
                        SchemaNode descendant = child.getChild(childName);
                        if (descendant != null) {
                            return descendant;
                        }
                    }
                }
                return null;
            }
        }
    }

    public String getPath() {
        return path;
    }

    public boolean isRecursive() {
        return recursiveParent != null;
    }

    public boolean isIndicator() {
        return indicator != null;
    }

    public IndicatorType getIndicator() {
        return indicator;
    }
    
    
    private void updatePath(String parentPath) {
        this.path = ("".equals(elementName) ? parentPath : parentPath + "/" + elementName);
        for (SchemaNode child : children) {
            child.updatePath(path);
        }
    }

    private void checkRecursion() {
        if (!isIndicator()) {
            SchemaNode ancestor = parent;
            while (ancestor != null) {
                if (!ancestor.isIndicator() && getGlobalDefinition().equals(ancestor.getGlobalDefinition())
                        && elementName.equals(ancestor.elementName)) {
                    this.w3cType = ancestor.w3cType;
                    this.recursiveParent = ancestor;
                    return;
                }
                ancestor = ancestor.parent;
            }
        }
        this.recursiveParent = null;
    }

    public boolean isLeaf() {
        return (children.isEmpty());
    }

    public boolean isAny() {
        return any;
    }

    public boolean isQualified() {
        return qualified;
    }

    public boolean isAttribute() {
        return attribute;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String getOptionalValue() {
        if (fixedValue != null)
            return fixedValue;
        else if (defaultValue != null)
            return defaultValue;
        else
            return unknownValue;
    }

    public String getFixedValue() {
        return fixedValue;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getCustomType() {
        return customType;
    }

    /**
     * Set the type of the element. Used for complex elements, whose type is explicitly defined.
     */
    public void setCustomType(String typeName) {
        this.customType = typeName;
        checkRecursion();
    }

    private void setUnknownValue(String x) {
        unknownValue = x;
    }
    
    public DataType getW3CType() {
        return w3cType;
    }
    
    void setMixed() {
        if (w3cType == DataType.COMPLEX)
            w3cType = DataType.MIXED;
    }

    /**
     * Set the simple type of the element, as it is in the XSD.
     * @param w3cTypeName localName of the XSD data type (e.g. nonNegativeInteger, dateTime, string)
     */
    void setW3CType(String w3cTypeName) {
        switch (w3cTypeName) {
            case "byte", "short", "int", "unsignedShort", "unsignedByte" -> {
                this.w3cType = DataType.INTEGER;
                setUnknownValue("0");
            }
            case "integer", "long", "nonNegativeInteger", "nonPositiveInteger", "unsignedLong", "unsignedInt" -> {
                this.w3cType = DataType.LONG;
                setUnknownValue("0");
            }
            case "positiveInteger" -> {
                this.w3cType = DataType.LONG;
                setUnknownValue("1");
            }
            case "negativeInteger" -> {
                this.w3cType = DataType.LONG;
                setUnknownValue("-1");
            }
            case "decimal", "double", "float" -> {
                this.w3cType = DataType.DOUBLE;
                setUnknownValue("0.0");
            }
            case "boolean" -> {
                this.w3cType = DataType.BOOLEAN;
                setUnknownValue("false");
            }
            case "date" -> {
                this.w3cType = DataType.DATE;
                setUnknownValue("2000-01-01");
            }
            case "dateTime" -> {
                this.w3cType = DataType.DATETIME;
                setUnknownValue("2000-01-01T00:00:00Z");
            }
            case "time" -> {
                this.w3cType = DataType.TIME;
                setUnknownValue("00:00:00");
            }
            case "duration" -> {
                this.w3cType = DataType.STRING;
                setUnknownValue("P0S");
            }
            case null, default -> this.w3cType = DataType.STRING;
        }
    }

    public String getDocumentation() {
        return documentation;
    }

    /**
     * Set the documentation of the element or attribute
     */
    void setDocumentation(String documentation) {
        if (documentation != null) {
            this.documentation = documentation;
        }
    }

    public Map<String, String> getRestrictions() {
        return restrictions == null ? immutableEmptyMap : Collections.unmodifiableMap(restrictions);
    }

    public String restrictionsToText() {
        return restrictions == null
            ? null
            : restrictions
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(e -> e.getKey() + ": '" + e.getValue() + "'")
                .collect(Collectors.joining("; "));
    }

    void appendRestrictions(String type, String value) {
        if (this.restrictions == null) {
            this.restrictions = new HashMap<>();
        }
        restrictions.compute(type, (t, v) -> (v == null) ? value : v + "|" + value);
    }

    public boolean isSimpleType() {
        return !(w3cType == DataType.COMPLEX || w3cType == DataType.ANY);
    }
    
    /**
     * Convenience method to get a textual representation of the cardinality (e.g. 1, 1..n, 0..3)
     */
    public String getCardinality() {
        String mo = (maxOccurs == UNBOUNDED_VALUE ? "n" : "" + maxOccurs);
        return minOccurs + (maxOccurs > minOccurs ? ".." + mo : "");
    }

    public SchemaNode clone(int minOccurs, int maxOccurs) {
        SchemaNode clone = new SchemaNode(elementName, indicator, namespace, qualified, attribute, any, defaultValue, fixedValue,
            minOccurs, maxOccurs);
        clone.w3cType = w3cType;
        clone.customType = customType;
        clone.unknownValue = unknownValue;
        if (restrictions != null) {
            clone.restrictions = new HashMap<>(restrictions);
        }

        for (SchemaNode child: children) {
            clone.addChild(child.clone(child.minOccurs, child.maxOccurs));
        }
        return clone;
    }
}
