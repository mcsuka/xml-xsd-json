package com.mcsuka.xml.xsd.model;

import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

public record SoapRestServiceDefinition(
    String restPath,
    String restMethod,
    WsdlDocumentSource wsdlSource,
    WsdlDocumentSource.SoapOperation operation
) {
}
