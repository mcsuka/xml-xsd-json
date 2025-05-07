package com.mcsuka.xml.http;

public record RestResponse(
    Integer status,
    String body
) {
}
