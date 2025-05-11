package com.mcsuka.xml.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcsuka.xml.http.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpContentResponse;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.util.StringRequestContent;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientHandler extends HandlerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final HttpClient client;
    private final Rest2SoapTransformer transformer;
    private final String oasDoc;

    private static final Gson GSONPretty = new GsonBuilder()
        .setPrettyPrinting()
        .create();


    public ClientHandler(ProxySettings settings, HttpClient httpClient) {
        List<SoapRestServiceDefinition> services = settings.services();
        client = httpClient;
        transformer = new Rest2SoapTransformer(services);

        JsonObject oas = OasGenerator.generateOas(services, "Proxy Service", "Genarated OAS Document", "0.1");
        oasDoc = GSONPretty.toJson(oas);
    }

    public void stopClient() throws Exception {
        client.stop();
        stop();
    }

    public void startClient() throws Exception {
        client.start();
        start();
    }

    @Override
    public void handle(String uri, final Request request, HttpServletRequest servletRequest,
                       HttpServletResponse servletResponse) throws IOException {

        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        servletResponse.setContentType("application/json");
        if (uri.equals("/oas.json") && "get".equalsIgnoreCase(servletRequest.getMethod())) {
            servletResponse.getWriter().write(oasDoc);
            servletResponse.setContentLength(servletResponse.getBufferSize());
            servletResponse.setStatus(200);
        } else {
            try {
                RestRequest restRequest = RestRequest.fromHttpRequest(servletRequest);
                SoapRequest soapRequest = transformer.transformRequest(restRequest);
                HttpRequest clientRequest = (HttpRequest) client.newRequest(soapRequest.serviceDef().getTargetUrl());
                clientRequest.method(HttpMethod.POST);
                clientRequest.version(HttpVersion.HTTP_1_1);
                clientRequest.addHeader(new HttpField("SOAPAction", "\"" + soapRequest.serviceDef().getSoapAction() + "\""));
                clientRequest.body(new StringRequestContent(soapRequest.contents()));
                HttpContentResponse clientResponse = (HttpContentResponse)clientRequest.send();
                SoapResponse soapResponse = new SoapResponse(clientResponse.getStatus(), new String(clientResponse.getContent()));
                RestResponse restResponse = transformer.transformResponse(soapRequest.serviceDef(), soapResponse);
                servletResponse.getWriter().write(restResponse.body());
                servletResponse.setContentLength(servletResponse.getBufferSize());
                servletResponse.setStatus(restResponse.status());

            } catch (Exception e) {
                logger.warn("Error processing REST request", e);
                JsonObject error = new JsonObject();
                error.addProperty("errorCode", "InternalError");
                // Error details sent for testing purposes
                // In a production environment internal error details should be suppressed
                error.addProperty("errorDescription", e.toString());
                servletResponse.getWriter().write(GSONPretty.toJson(error));
                servletResponse.setContentLength(servletResponse.getBufferSize());
                servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        }
        request.setHandled(true);
        servletResponse.flushBuffer();
    }

}
