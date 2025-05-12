package com.mcsuka.xml.http;

import org.jetbrains.annotations.NotNull;

public record SoapRequest(
    @NotNull SoapRestServiceDefinition serviceDef,
    @NotNull String contents
) {
}
