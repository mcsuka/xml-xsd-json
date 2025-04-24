package com.mcsuka.xml.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcsuka.xml.testtools.GenericTools;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class TestOasGenerator {

    private static final Gson GSON = new GsonBuilder()
        .create();

    private static final Gson GSONPretty = new GsonBuilder()
        .setPrettyPrinting()
        .create();


    private final RequestParameter correlationIdHeader = new RequestParameter(
        "X-Correlation-Id",
        "header",
        Map.of("type", "string"),
        false,
        false,
        null,
        "Correlation ID");

    @Test
    public void testWsdl() throws Exception {
        SoapRestServiceDefinition service = new SoapRestServiceDefinition(
            "http://dummy.net/soap",
            "/oneservice",
            "post",
            List.of(correlationIdHeader),
            new WsdlDocumentSource("file://testdata/input/OneService.wsdl"),
            "Operation",
            "A test SOAP Service");
        String expected = GenericTools.getResourceFile("testdata/output/OneService.oas.json");

        var oas = OasGenerator.generateOas(List.of(service), "Test API", "This is a generated REST API", "0.0.1");

        Assertions.assertEquals(expected, GSON.toJson(oas));
    }

    @Test
    public void testWsdlECommerce() throws Exception {
        SoapRestServiceDefinition service = new SoapRestServiceDefinition(
            "http://dummy.net/soap",
            "/order",
            "post",
            List.of(correlationIdHeader),
            new WsdlDocumentSource("file://testdata/input/eCommerce.wsdl"),
            "PlaceOrder",
            "A test SOAP Service");
        String expected = GenericTools.getResourceFile("testdata/output/OneService.oas.json");

        var oas = OasGenerator.generateOas(List.of(service), "Test API", "This is a generated REST API", "0.0.1");

        System.out.println(GSONPretty.toJson(oas));
    }

}
