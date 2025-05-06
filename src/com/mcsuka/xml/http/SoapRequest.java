package com.mcsuka.xml.http;

public record SoapRequest(
    String path,
    String soapAction,
    String contents
) {
}
