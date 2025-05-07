package com.mcsuka.xml.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static com.mcsuka.xml.http.SoapServices.*;

public class TestRest2SoapTransformer {


    @Test
    public void testECommerceGetRequest() throws Exception {

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommerceGet));

        NavigableMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("X-Correlation-Id", "abcdefghijk");
        RestRequest restRequest = new RestRequest("get", "/order/123456", null, null, headers);

        var soapRequest = transformer.transformRequest(restRequest);
        Assertions.assertEquals(ECommerceGet, soapRequest.serviceDef());
        Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><ns0:ProductReference xmlns:ns0=\"http://example.com/ecommerce/schema\"><ns0:ProductId>123456</ns0:ProductId></ns0:ProductReference></SOAP-ENV:Body></SOAP-ENV:Envelope>", soapRequest.contents());
    }

    @Test
    public void testECommercePostRequest() throws Exception {

        String body = """
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
            }""";

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommercePost, ECommerceGet));

        NavigableMap<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("X-Correlation-Id", "abcdefghijk");
        RestRequest restRequest = new RestRequest("post", "/order", null, body, headers);

        var soapRequest = transformer.transformRequest(restRequest);

        Assertions.assertEquals(ECommercePost, soapRequest.serviceDef());
        Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Body><ns0:Order xmlns:ns0=\"http://example.com/ecommerce/schema\"><ns0:OrderId>24252542</ns0:OrderId><ns0:CustomerName>Joe</ns0:CustomerName><ns0:Products><ns0:Product><ns0:ProductId>prod-001</ns0:ProductId><ns0:ProductName>Red Apple</ns0:ProductName><ns0:Price>1.23</ns0:Price></ns0:Product><ns0:Product><ns0:ProductId>prod-002</ns0:ProductId><ns0:ProductName>Green Apple</ns0:ProductName><ns0:Price>1.52</ns0:Price></ns0:Product></ns0:Products></ns0:Order></SOAP-ENV:Body></SOAP-ENV:Envelope>", soapRequest.contents());
    }

    @Test
    public void testECommerceGetResponse() throws Exception {
        String soapResponseBody = """
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                <SOAP-ENV:Body>
                    <Product xmlns="http://example.com/ecommerce/schema">
                        <ProductId>31415</ProductId>
                        <ProductName>Fidget Spinner</ProductName>
                        <Price>13.14</Price>
                    </Product>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            """;
        String expectedRestResponseBody = "{\"ProductId\":\"31415\",\"ProductName\":\"Fidget Spinner\",\"Price\":\"13.14\"}";

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommercePost, ECommerceGet));
        SoapResponse soapResponse = new SoapResponse(200, soapResponseBody);
        RestResponse restResponse = transformer.transformResponse(ECommercePost, soapResponse);

        Assertions.assertEquals(200, restResponse.status());
        Assertions.assertEquals(expectedRestResponseBody, restResponse.body());
    }

    @Test
    public void testECommerceGetResponseError() throws Exception {
        String soapResponseBody = """
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
                <SOAP-ENV:Body>
                    <SOAP-ENV:Fault>
                        <faultcode>SOAP-ENV:Client</faultcode>
                        <faultstring>Forbidden</faultstring>
                        <detail>
                            <code>9876543</code>
                            <description>Lorem ipsum</description>
                        </detail>
                    </SOAP-ENV:Fault>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            """;
        String expectedRestResponseBody = "{\"faultcode\":\"SOAP-ENV:Client\",\"faultstring\":\"Forbidden\",\"detail\":{\"code\":\"9876543\",\"description\":\"Lorem ipsum\"}}";

        Rest2SoapTransformer transformer = new Rest2SoapTransformer(List.of(ECommercePost, ECommerceGet));
        SoapResponse soapResponse = new SoapResponse(500, soapResponseBody);
        RestResponse restResponse = transformer.transformResponse(ECommercePost, soapResponse);

        Assertions.assertEquals(500, restResponse.status());
        Assertions.assertEquals(expectedRestResponseBody, restResponse.body());
    }}
