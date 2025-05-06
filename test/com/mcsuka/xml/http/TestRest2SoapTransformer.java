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
import java.util.List;

import static com.mcsuka.xml.http.SoapServices.*;

public class TestRest2SoapTransformer {

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
    public void testECommerceGetRequest() throws Exception {

        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getRequestURI()).thenReturn("/order/123456");
        Mockito.when(mockRequest.getMethod()).thenReturn("get");
        Mockito.when(mockRequest.getQueryString()).thenReturn(null);
        Mockito.when(mockRequest.getHeader("X-Correlation-Id")).thenReturn("abcdefghijk");
        Mockito.when(mockRequest.getInputStream()).thenReturn(null);

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommerceGet));

        var soapRequest = transformer.transformRequest(mockRequest);
        Assertions.assertEquals("http://dummy.net/soap", soapRequest.path());
        Assertions.assertEquals("http://example.com/ecommerce/GetProduct", soapRequest.soapAction());
        Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><ns0:ProductReference xmlns:ns0=\"http://example.com/ecommerce/schema\"><ns0:ProductId>123456</ns0:ProductId></ns0:ProductReference></SOAP-ENV:Body></SOAP-ENV:Envelope>", soapRequest.contents());
    }

    @Test
    public void testECommercePostRequest() throws Exception {

        var is = new ByteArrayInputStream("""
            {
                "OrderId": "24252542",
                "CustomerName": "Joe",
                "Products": {
                    "Product": [{
                        "ProductId": "prod-001",
                        "ProductName": "Red Apple",
                        "Price": 1.23
                    },
                    {
                        "ProductId": "prod-002",
                        "ProductName": "Green Apple",
                        "Price": 1.52
                    }]
                }
            }""".getBytes());
        ServletInputStream sis = mockServletInputStream(is);


        HttpServletRequest mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getRequestURI()).thenReturn("/order");
        Mockito.when(mockRequest.getMethod()).thenReturn("post");
        Mockito.when(mockRequest.getQueryString()).thenReturn(null);
        Mockito.when(mockRequest.getHeader("X-Correlation-Id")).thenReturn("abcdefghijk");
        Mockito.when(mockRequest.getInputStream()).thenReturn(sis);

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommercePost, ECommerceGet));

        var soapRequest = transformer.transformRequest(mockRequest);
        Assertions.assertEquals("http://dummy.net/soap", soapRequest.path());
        Assertions.assertEquals("http://example.com/ecommerce/PlaceOrder", soapRequest.soapAction());
        Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><ns0:Order xmlns:ns0=\"http://example.com/ecommerce/schema\"><ns0:OrderId>24252542</ns0:OrderId><ns0:CustomerName>Joe</ns0:CustomerName><ns0:Products><ns0:Product><ns0:ProductId>prod-001</ns0:ProductId><ns0:ProductName>Red Apple</ns0:ProductName><ns0:Price>1.23</ns0:Price></ns0:Product><ns0:Product><ns0:ProductId>prod-002</ns0:ProductId><ns0:ProductName>Green Apple</ns0:ProductName><ns0:Price>1.52</ns0:Price></ns0:Product></ns0:Products></ns0:Order></SOAP-ENV:Body></SOAP-ENV:Envelope>", soapRequest.contents());
    }


}
