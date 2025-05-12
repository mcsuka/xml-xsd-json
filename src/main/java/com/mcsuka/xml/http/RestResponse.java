package com.mcsuka.xml.http;

public record RestResponse(
    int status,
    String body
) {
}
