package com.mcsuka.xml.http;

import com.mcsuka.xml.xsd.model.SchemaNode;
import com.mcsuka.xml.xsd.model.SchemaParser;
import com.mcsuka.xml.xsd.model.SchemaParserFactory;
import com.mcsuka.xml.xsd.tools.WsdlDocumentSource;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class SoapRestServiceDefinition {

    private final String restPath;
    private final String restMethod;
    private final List<RequestParameter> requestParameters;
    private final WsdlDocumentSource wsdlSource;
    private final String operationName;
    private final String description;
    private final String targetUrl;
    private final String pattern;

    public String getRestPath() {
        return restPath;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getRestMethod() {
        return restMethod;
    }

    public List<RequestParameter> getRequestParameters() {
        return requestParameters;
    }

    public WsdlDocumentSource getWsdlSource() {
        return wsdlSource;
    }

    public String getOperationName() {
        return operationName;
    }

    public String getDescription() {
        return description;
    }

    public SoapRestServiceDefinition(
        String targetUrl,
        String restPath,
        String restMethod,
        List<RequestParameter> requestParameters,
        WsdlDocumentSource wsdlSource,
        String operationName,
        String description
    ) {
        this.targetUrl = targetUrl;
        this.restPath = restPath;
        this.restMethod = restMethod;
        this.requestParameters = requestParameters;
        this.wsdlSource = wsdlSource;
        this.operationName = operationName;
        this.description = description;
        this.pattern = restPath.replaceAll("\\{[a-zA-Z0-9_]+\\}", "[a-zA-Z0-9_]+");

        this.pathParamIndex = new HashMap<>();
        String[] path = restPath.split("/");
        for (int i = 0; i < path.length; i++) {
            if (path[i].matches("\\{[a-zA-Z0-9_]+\\}")) {
                pathParamIndex.put(path[i].substring(1, path[i].length() - 1), i);
            }
        }

        requestSchema = getSchema(WsdlDocumentSource.SoapOperation::requestRootElement);
        responseSchema = getSchema(WsdlDocumentSource.SoapOperation::responseRootElement);
    }

    private final Map<String, Integer> pathParamIndex;
    private SchemaNode requestSchema;
    private SchemaNode responseSchema;

    public boolean match(RestRequest restRequest) {
        return restRequest.method().equals(restMethod) && restRequest.requestUri().matches(pattern);
    }

    public Optional<Integer> getPathParamIndex(String paramName) {
        return Optional.ofNullable(pathParamIndex.get(paramName));
    }

    public String getSoapAction() {
        return wsdlSource
            .getOperation(operationName)
            .map(WsdlDocumentSource.SoapOperation::soapAction)
            .orElse("");
    }

    public Optional<SchemaNode> getRequestSchema() {
        return Optional.ofNullable(requestSchema);
    }

    public Optional<SchemaNode> getResponseSchema() {
        return Optional.ofNullable(responseSchema);
    }

    private SchemaNode getSchema(Function<WsdlDocumentSource.SoapOperation, QName> rootElemResolver) {
        return wsdlSource
            .getOperation(operationName)
            .map(rootElemResolver)
            .map(requestQname -> {
                try {
                    SchemaParser sp = SchemaParserFactory.newSchemaParser(requestQname.getNamespaceURI(), wsdlSource);
                    return sp.parse(requestQname.getLocalPart());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .orElse(null);
    }

}
