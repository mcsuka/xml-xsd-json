package com.mcsuka.xml.http;

import java.util.Map;
import java.util.List;

public record SoapRequest(
    String path,
    String soapAction,
    Map<String, String> headers,
    String body
) {
}
