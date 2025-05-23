# xml-xsd-json
Generic library to
* parse and visualize XML Schemas
* transform JSON to XML, guided by XML Schema
* transform XML to JSON, guided by XML schema
* expose SOAP services as REST/JSON, including
  * generating OAS 3.0 API spec from the WSDLs
  * transforming the REST request to SOAP request
  * transforming the SOAP response as REST response

Demo REST to SOAP proxy service, with embedded Jetty HTTP server/client.

Please note all code is provided as-is, without any warranty.

## XML Schema Visualization
implementation: com.mcsuka.xml.xsd.SchemaParser\
The  XML Schema parser converts an XSD file (or a number of related XSD files) to a simple tree of 'SchemaNode' objects. Each SchemaNode represents one of the following:
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
  * qualified - whether the element or attribute should be placed in its name space
  * attribute - whether it is an attribute
  * any - whether it is an "any" type
* w3cType - the basic type of the element or attribute, or COMPLEX/MIXED for complex types
* customType - the localName of the explicitly defined type
* documentation - annotation/documentation of the element or type
* restrictions - restrictions applied on the type, in human-readable format

The XML schema can be visualized as a text document using the `dumpTree()` convenience function of the SchemaNode class. Other visualizations (HTML, graphic, ...) can be easily implemented as separate classes.

## XML to JSON translation
implementation: com.mcsuka.xml.json.Xml2Json\
Translation may or may not use an XML Schema. Using an XML schema has the following advantages:
* keep types: the resulting JSON will contain boolean and numeric values without quotes
* keep arrays: the resulting JSON will contain array for keys that are defined as repeatable elements in the XSD, even if there is only one element in the input XML.
  * please note, you may also use the `_jsonarray="true"` attribute in the input XML to force array creation

Disadvantage of using an XML Schema:
* slightly slower translation

## JSON to XML translation
implementation: com.mcsuka.xml.json.Json2Xml\
Translation may or may not use an XML Schema. Using an XML schema has the following advantages:
* add correct name space to the elements and attributes
* correct the order of the elements for 'sequence' indicator
* create missing, mandatory elements and attributes:
  * missing elements and attributes are created with their fixed or default values. If there is no default value defined, a dummy value is chosen, according to the data type
* remove invalid elements:
  * not in the schema
  * over the 'maxOccurs' limit
  * multiple elements inside a choice indicator

Disadvantage of using an XML Schema:
* slightly slower translation

JSON to XML translation will modify the JSON keys that are not XML element-name compatible. All non-compatible characters will be replaced with an underscore character. Please note, there is no guarantee that the XML produced by the translator is valid (to the schema).

## XML Schema to JSON Schema translation
implementation: com.mcsuka.xml.json.Xsd2JsonSchema

A JSON Schema is a type of visualisation for an XML Schema, representing the output/input format of a schema-based XMLtoJSON/JSONtoXML translation.
The schemas may come from a set of XSD files or a WSDL.

## REST to SOAP translation
implementation: com.mcsuka.xml.http.Rest2SoapTransformer, com.mcsuka.xml.http.OasGenerator\
This library enables exposing legacy SOAP services as a REST API. The REST API is a collection of endpoints, each endpoint configured by:
* a URL to the SOAP WSDL (this may be the same for multiple or all of the endpoints)
* the SOAP operation name in the WSDL (API specification is constructed from the WSDL)
* the URL of the SOAP service
* the Path where the REST endpoint is exposed (this may contain path variables)
* the HTTP method of the REST endpoint (get/post/put/patch/delete)
* a list of REST request parameters
  * these may be path, query or header variables
  * they can be mapped to an XML element in the SOAP request
  * their type can be specified

Limitations:
* The SOAP messages (in/out) must contain at least 2 levels of elements
  * e.g. `<SOAP-ENV:Body><ns0:root><value>42</value></ns0:root></SOAP-ENV:Body>`
  * the following will be invalid: `<SOAP-ENV:Body><value>42</value></SOAP-ENV:Body>`

## Demo REST to SOAP Proxy application
implementation: com.mcsuka.xml.proxy.RestToSoapProxyApp\
This is a simple, configurable proxy application that can expose SOAP services as a REST API:
* The API is configured via a single .properties file and SOAP WSDL(s)
* The API specification is exposed as an OAS 3.0 document
* It contains a Swagger distributable UI for easy testing

Please note, this is a demo application, not advised for production use. It is meant to give guidance on how to expose legacy SOAP services with minimum effort as a REST API,  in your own choice of Java, Scala or Kotlin frameworks.

compile: `mvn clean compile assembly:single`

run: `java -cp target/xmlxsdjson-0.1-jar-with-dependencies.jar com.mcsuka.xml.proxy.RestToSoapProxyApp "config/resttosoapproxy.properties"`

OAS: `http://localhost:8080/oas.json` \
Swagger UI: `http://localhost:8080/swagger/`

## Demo SOAP Service
implementation: in the separate soapserver module\
This is a modified copy of the WSDL First Demo of the Apache CXF project (https://github.com/apache/cxf). For more info, please refer to [soapserver/README.txt](soapserver/README.txt)
