package com.mcsuka.xml.xsd.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OasGenerator {

    public static String generateOas(List<SoapRestServiceDefinition> services) {

        return null; // ToDo
    }

//    private static JsonObject renderSchema(JsonArray input) {
//        JsonObject schema = new JsonObject();
//        schema.addProperty("type", "array");
//        if (!input.isEmpty() && input.get(0) instanceof JsonObject) {
//            schema.add("items", renderSchema((JsonObject) input.get(0)));
//        } else {
//            JsonObject type = new JsonObject();
//            type.addProperty("type", "string");
//            schema.add("items", type);
//        }
//        return schema;
//    }



    public static JsonObject oasService(SoapRestServiceDefinition serviceDef) {
        JsonObject service = new JsonObject();
        service.addProperty("summary", serviceDef.description());
        JsonArray parameters = new JsonArray();
        serviceDef.requestParameters().forEach(paramDef -> {
            parameters.add(paramDef.asOasServiceParam());
        });


        return service;
    }


}
