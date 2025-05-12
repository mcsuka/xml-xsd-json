package com.mcsuka.xml.proxy;

import com.mcsuka.xml.http.RequestParameter;
import com.mcsuka.xml.http.SoapRestServiceDefinition;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

import java.util.*;
import java.util.stream.Collectors;

public record ProxySettings(
    String serverHost,
    Integer serverPort,
    Integer maxServerPoolSize,
    Integer maxClientPoolSize,
    Integer serverKeepAliveTimeMs,
    Integer clientKeepAliveTimeMs,
    Integer connectTimeoutMs,
        List<SoapRestServiceDefinition> services
){
    static ProxySettings propsToSettings(Properties props) throws Exception {
        List<SoapRestServiceDefinition> services = new ArrayList<>();

        for(String key: props.stringPropertyNames()) {
            if (key.matches("^rest2soap.service\\.[A-Za-z0-9_]+\\.restPath$")) {
                String prefix = key.substring(0, key.indexOf(".restPath"));
                services.add(propsToServiceDef(props, prefix));
            }
        }

        return new ProxySettings(
            props.getProperty("server.host", "0.0.0.0"),
            Integer.parseInt(props.getProperty("server.port", "8080")),
            Math.max(2, Integer.parseInt(props.getProperty("server.maxPoolSize", "8"))),
            Math.max(2, Integer.parseInt(props.getProperty("client.maxPoolSize", "4"))),
            Integer.parseInt(props.getProperty("server.keepAliveTimeMs", "1000")),
            Integer.parseInt(props.getProperty("client.keepAliveTimeMs", "1000")),
            Integer.parseInt(props.getProperty("client.connectTimeoutMs", "5000")),
            services
        );
    }


    static SoapRestServiceDefinition propsToServiceDef(Properties props, String prefix) throws Exception {

        String[] params = props.getProperty(prefix + ".paramList").split("[|]");
        List<RequestParameter> requestParams = Arrays.stream(params)
            .filter(s -> !s.isBlank())
            .map(param ->
                new RequestParameter(
                    props.getProperty("rest.params." + param + ".name"),
                    props.getProperty("rest.params." + param + ".paramType"),
                    parseOasTypeDef(props.getProperty("rest.params." + param + ".oasTypeDef")),
                    Boolean.parseBoolean(props.getProperty("rest.params." + param + ".multiValue", "false")),
                    Boolean.parseBoolean(props.getProperty("rest.params." + param + ".required", "false")),
                    props.getProperty("rest.params." + param + ".jsonPath", "").split("\\."),
                    props.getProperty("rest.params." + param + ".description")
                ))
            .toList();

        return new SoapRestServiceDefinition(
            props.getProperty(prefix + ".targetUrl"),
            props.getProperty(prefix + ".restPath"),
            props.getProperty(prefix + ".restMethod"),
            requestParams,
            new WsdlDocumentSource(props.getProperty(prefix + ".wsdlUrl")),
            props.getProperty(prefix + ".operationName"),
            props.getProperty(prefix + ".description"));
    }

    static Map<String,String> parseOasTypeDef(String propValue) {
        if (propValue != null && !propValue.isBlank()) {
            return Arrays.stream(propValue.split("[|]"))
                .map(token -> token.split(":"))
                .filter(kv -> kv.length == 2 && !kv[0].isBlank() && !kv[1].isBlank())
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
        }
        return Map.of();
    }

}
