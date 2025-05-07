package com.mcsuka.xml.http;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

public record SoapResponse(
    int status,
    @NotNull String body
) {
}
