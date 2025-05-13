package com.mcsuka.xml.http;

import org.jetbrains.annotations.NotNull;

public record SoapRequest(
    @NotNull SoapRestServiceDefinition serviceDef,
    @NotNull String contents
) {

    @Override
    public @NotNull String toString() {
        return "URL: " + serviceDef.getTargetUrl() + "SOAPAction: " + serviceDef.getSoapAction() + " Contents: " + contents;
    }

}
