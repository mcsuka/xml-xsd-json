package com.mcsuka.xml.http;

import org.jetbrains.annotations.NotNull;

public record RestResponse(
    int status,
    String body
) {

    @Override
    public @NotNull String toString() {
        return "Status: " + status + " Body: " + body;
    }

}
