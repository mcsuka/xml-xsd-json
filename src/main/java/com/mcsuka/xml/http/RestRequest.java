package com.mcsuka.xml.http;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.NavigableMap;
import java.util.TreeMap;

public record RestRequest(
    @NotNull String method,
    @NotNull String requestUri,
    String queryString,
    String body,
    @NotNull NavigableMap<String, String> headers
) {

    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    public static RestRequest fromHttpRequest(HttpServletRequest servletRequest) throws IOException {
        String requestUri = servletRequest.getRequestURI();
        String method = servletRequest.getMethod().toLowerCase();
        String queryString = servletRequest.getQueryString();
        String body = getRequestBody(servletRequest);
        NavigableMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        servletRequest.getHeaderNames().asIterator().forEachRemaining(headerName ->
            headers.put(headerName, servletRequest.getHeader(headerName))
        );
        return new RestRequest(method, requestUri, queryString, body, headers);
    }

    static String getRequestBody(HttpServletRequest request) throws IOException {
        try (InputStream inputStream = request.getInputStream()) {
            if (inputStream != null) {
                byte[] buf = inputStream.readAllBytes();
                return new String(buf);
            } else {
                return "";
            }
        }
    }

    @Override
    public @NotNull String toString() {
        return "URI: " + method + " " + requestUri + " Query: " + queryString + " Headers: " + headers + " Body: " + body;
    }

}
