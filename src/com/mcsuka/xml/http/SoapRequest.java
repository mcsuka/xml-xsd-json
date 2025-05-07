package com.mcsuka.xml.http;

public record SoapRequest(
    SoapRestServiceDefinition serviceDef,
    String contents
) {
}
