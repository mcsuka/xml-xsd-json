package com.mcsuka.xml.http;

import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

import java.util.List;
import java.util.Map;

public class SoapServices {

    public static final RequestParameter CorrelationIdHeader = new RequestParameter(
        "X-Correlation-Id",
        "header",
        Map.of("type", "string"),
        false,
        false,
        null,
        "Correlation ID");

    public static final RequestParameter ProductIdPath = new RequestParameter(
        "productId",
        "path",
        Map.of("type", "string"),
        false,
        true,
        new String[]{"ProductId"},
        "Correlation ID");

    public static final SoapRestServiceDefinition OneService;
    public static final SoapRestServiceDefinition ECommerceGet;
    public static final SoapRestServiceDefinition ECommercePost;

    static {
        try {
            OneService = new SoapRestServiceDefinition(
                "http://dummy.net/soap",
                "/oneservice",
                "post",
                List.of(CorrelationIdHeader),
                new WsdlDocumentSource("file://testdata/input/OneService.wsdl"),
                "Operation",
                "A test SOAP Service");

            ECommerceGet = new SoapRestServiceDefinition(
                "http://dummy.net/soap",
                "/order/{productId}",
                "get",
                List.of(CorrelationIdHeader, ProductIdPath),
                new WsdlDocumentSource("file://testdata/input/eCommerce.wsdl"),
                "GetProduct",
                "GetProduct");

            ECommercePost = new SoapRestServiceDefinition(
                "http://dummy.net/soap",
                "/order",
                "post",
                List.of(CorrelationIdHeader),
                new WsdlDocumentSource("file://testdata/input/eCommerce.wsdl"),
                "PlaceOrder",
                "PlaceOrder");


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



}
