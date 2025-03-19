package com.mcsuka.xml.xsd.model;

import com.google.gson.JsonElement;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

import java.util.List;

public record SoapRestServiceDefinition(
    String restPath,
    String restMethod,
    List<RequestParameter> requestParameters,
    WsdlDocumentSource wsdlSource,
    JsonElement requestTemplate,
    String operationName,
    String description
) {
}
