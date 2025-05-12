package com.mcsuka.xml.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcsuka.xml.http.*;
import jakarta.servlet.ServletException;
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
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClientHandler extends HandlerWrapper {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final HttpClient client;
    private final WebAppContext swaggerUI;
    private final Rest2SoapTransformer transformer;
    private final String oasDoc;

    private static final Gson GSONPretty = new GsonBuilder()
        .setPrettyPrinting()
        .create();


    public ClientHandler(ProxySettings settings, HttpClient httpClient, WebAppContext swaggerUI) {
        List<SoapRestServiceDefinition> services = settings.services();
        this.client = httpClient;
        this.swaggerUI = swaggerUI;
        this.transformer = new Rest2SoapTransformer(services);

        JsonObject oas = OasGenerator.generateOas(services, "Proxy Service", "Genarated OAS Document", "0.1");
        this.oasDoc = GSONPretty.toJson(oas);
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
                       HttpServletResponse servletResponse) throws IOException, ServletException {

        servletResponse.setContentType("application/json");
        servletResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (uri.equals("/oas.json") && "get".equalsIgnoreCase(servletRequest.getMethod())) {
            servletResponse.getWriter().write(oasDoc);
            servletResponse.setStatus(200);
        } else if (uri.startsWith("/swagger")) {
            swaggerUI.handle(uri, request, servletRequest, servletResponse);
        } else if (uri.startsWith("/favicon.ico")) {
            try(InputStream iconStream = ClassLoader.getSystemResource("favicon.ico").openStream()) {
                servletResponse.setContentType("image/x-icon");
                servletResponse.getOutputStream().write(iconStream.readAllBytes());
                servletResponse.setStatus(200);
            }
        } else {
            try {
                RestRequest restRequest = RestRequest.fromHttpRequest(servletRequest);
                SoapRequest soapRequest = transformer.transformRequest(restRequest);
                logger.info(soapRequest.serviceDef().getTargetUrl());
                logger.info(soapRequest.serviceDef().getSoapAction());
                logger.info(soapRequest.contents());
                HttpRequest clientRequest = (HttpRequest) client.newRequest(soapRequest.serviceDef().getTargetUrl());
                clientRequest.method(HttpMethod.POST);
                clientRequest.version(HttpVersion.HTTP_1_1);
                clientRequest.addHeader(new HttpField("SOAPAction", "\"" + soapRequest.serviceDef().getSoapAction() + "\""));
                clientRequest.body(new StringRequestContent(soapRequest.contents()));
                HttpContentResponse clientResponse = (HttpContentResponse)clientRequest.send();
                SoapResponse soapResponse = new SoapResponse(clientResponse.getStatus(), new String(clientResponse.getContent()));
                logger.info(soapResponse.body());
                RestResponse restResponse = transformer.transformResponse(soapRequest.serviceDef(), soapResponse);
                logger.info(restResponse.body());
                servletResponse.getWriter().write(restResponse.body());
                servletResponse.setStatus(restResponse.status());

            } catch (Exception e) {
                logger.warn("Error processing REST request", e);
                JsonObject error = new JsonObject();
                error.addProperty("errorCode", "InternalError");
                // Error details sent for testing purposes
                // In a production environment internal error details should be suppressed
                error.addProperty("errorDescription", e.toString());
                servletResponse.getWriter().write(GSONPretty.toJson(error));
                servletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            }
        }
        request.setHandled(true);
        servletResponse.flushBuffer();
    }

}
