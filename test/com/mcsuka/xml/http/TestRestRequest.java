package com.mcsuka.xml.http;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class TestRestRequest {

    static ServletInputStream mockServletInputStream(final InputStream wrappedIS) {
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // do nothin'
            }

            @Override
            public int read() throws IOException {
                return wrappedIS.read();
            }

            @Override
            public byte @NotNull [] readAllBytes() throws IOException {
                return wrappedIS.readAllBytes();
            }

            @Override
            public void close() throws IOException {
                super.close();
                wrappedIS.close();
            }
        };
    }

    @Test
    public void testFromHttpRequest() throws IOException {
        String body = """
            {
                "OrderId": "24252542",
                "CustomerName": "Joe"
            }""";
        var is = new ByteArrayInputStream(body.getBytes());
        ServletInputStream sis = mockServletInputStream(is);


        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getRequestURI()).thenReturn("/order");
        Mockito.when(mockRequest.getMethod()).thenReturn("post");
        Mockito.when(mockRequest.getQueryString()).thenReturn("a=1&b=2");
        Mockito.when(mockRequest.getHeader("X-Correlation-Id")).thenReturn("abcdefghijk");
        Mockito.when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(List.of("X-Correlation-Id")));
        Mockito.when(mockRequest.getInputStream()).thenReturn(sis);

        RestRequest restRequest = RestRequest.fromHttpRequest(mockRequest);

        Assertions.assertEquals("post", restRequest.method());
        Assertions.assertEquals("/order", restRequest.requestUri());
        Assertions.assertEquals("a=1&b=2", restRequest.queryString());
        Assertions.assertEquals("abcdefghijk", restRequest.getHeader("x-coRRelatIon-id"));
        Assertions.assertEquals(1, restRequest.headers().size());
        Assertions.assertEquals(body, restRequest.body());
    }
}
