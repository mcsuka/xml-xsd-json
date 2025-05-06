package com.mcsuka.xml.http;

import java.util.List;
import java.util.Map;

public record SoapResponse(
    int status,
    Map<String, List<String>> headers,
    String body
) {
}
