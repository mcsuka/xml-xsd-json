package com.mcsuka.xml.xsd.model;

import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

import java.util.List;

public record SoapRestServiceDefinition(
    String restPath,
    String restMethod,
    List<RequestParameter> requestParameters,
    WsdlDocumentSource wsdlSource,
    WsdlDocumentSource.SoapOperation operation,
    String description
) {
}
