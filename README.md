# xml-xsd-json
XML Schema visualization and XML Schema based XML &lt;-> JSON conversion

## XML Schema Visualization
The com.mcsuka.xml.xsd package implements an XML Schema parser. It converts an XSD file (or a number of related XSD files) to a simple tree of 'SchemaNode' objects. Each SchemaNode represents one of the following:
* an element: xsd:element or xsd:any
* an attribute: xsd:attribute or xsd:anyAttribute
* an indicator: xsd:sequence xsd:all or xsd:choice

Each SchemaNode has the following properties: 
* elementName - the localName of an element or attribute, '' for an indicator, '\*' for an any element or attribute
* namespace - name space of the element or attribute
* indicator - type of indicator, for indicator nodes
* defaultValue default value of the element or attribute, null if there is no default value. If no default value is defined then a dummy default value is assigned to every simple type, to allow auto-generation of the XML element
* fixedValue fixed value of the element or attribute, null if there is no default value
* minOccurs minimum occurrence of the element, attribute or indicator
* maxOccurs maximum occurrence of the element, attribute or indicator. Integer.MAX_VALUE is used to define 'unbounded'
* flags to define a boolean property of the SchemaNode:
** qualified - whether the element or attribute should be placed in its name space
** attribute - whether it is an attribute
** any whether it is an "any" type
* w3cType - the basic type of the element or attribute, or COMPLEX/MIXED for complex types
* customType - the localName of the explicitly defined type
* documentation - annotation/documentation of the element or type
* restrictions - restrictions applied on the type, in human-readable format

The XML schema can be visualized as a text document using the `dumpTree()` convenience function of the SchemaNode class. Other visualizations (HTML, graphic, ...) can be easily implemented as separate classes.

## XML to JSON translation
The com.mcsuka.xml.json package implements an XML/JSON translator, using the GSON, Xerces and Saxon-HE libraries.
Translation may or may not use an XML Schema. Using an XML schema has the following advantages:
* keep types: the resulting JSON will contain boolean and numeric values without quotes
* keep arrays: the resulting JSON will contain array for keys that are defined as repeatable elements in the XSD, even if there is only one element in the input XML.
** please note, you may also use the `_jsonarray="true"` attribute in the input XML to force array creation
Disadvattages of using an XML Schema:
* slightly slower translation

## JSON to XML translation
The com.mcsuka.xml.json package implements a JSON/XML translator, using the GSON, Xerces and Saxon-HE libraries.
Translation may or may not use an XML Schema. Using an XML schema has the following advantages:
* add correct namespace to the elements and attributes
* correct the order of the elements for 'sequence' indicator
* create missing, mandatory elements and attributes:
** missing elements and attributes are created with their fixed or default values. If there is no default value defined, a dummy value is chosen, according to the data type
* remove invalid elements:
** not in the schema
** over the 'maxOccurs' limit
** multiple elements inside a choice indicator
Disadvattages of using an XML Schema:
* slightly slower translation

JSON to XML translation will modify the JSON keys that are not XML element-name compatible. All non-compatible characters will be replaced with an underscore character. Please note, there is no guarantee that the XML produced by the translator is valid (to the schema).
