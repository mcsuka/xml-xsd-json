package com.mcsuka.xml.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcsuka.xml.testtools.GenericTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mcsuka.xml.http.SoapServices.*;

public class TestOasGenerator {

    private static final Gson GSON = new GsonBuilder()
        .create();

    private static final Gson GSONPretty = new GsonBuilder()
        .setPrettyPrinting()
        .create();


    @Test
    public void testWsdl() throws Exception {
        String expected = GenericTools.getResourceFile("testdata/output/OneService.oas.json");

        var oas = OasGenerator.generateOas(List.of(OneService), "Test API", "This is a generated REST API", "0.0.1");

        Assertions.assertEquals(expected, GSON.toJson(oas));
    }

    @Test
    public void testWsdlECommercePost() throws Exception {
        String expected = GenericTools.getResourceFile("testdata/output/ECommercePost.oas.json");

        var oas = OasGenerator.generateOas(List.of(ECommercePost), "Test API", "This is a generated REST API", "0.0.1");

        Assertions.assertEquals(expected, GSON.toJson(oas));
    }

    @Test
    public void testWsdlECommerceGet() throws Exception {
        String expected = GenericTools.getResourceFile("testdata/output/ECommerceGet.oas.json");

        var oas = OasGenerator.generateOas(List.of(ECommerceGet), "Test API", "This is a generated REST API", "0.0.1");

        Assertions.assertEquals(expected, GSON.toJson(oas));
    }

}
