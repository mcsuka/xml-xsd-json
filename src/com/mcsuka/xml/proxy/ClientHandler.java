package com.mcsuka.xml.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mcsuka.xml.http.OasGenerator;
import com.mcsuka.xml.http.SoapRestServiceDefinition;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ClientHandler extends HandlerWrapper {

    private final HttpClient client;
    private final List<SoapRestServiceDefinition> services;
    private final byte[] oasDoc;

    private static final Gson GSONPretty = new GsonBuilder()
        .setPrettyPrinting()
        .create();


    public ClientHandler(ProxySettings settings) {
        services = settings.services();
        client = createHttpClient(settings);

        JsonObject oas = OasGenerator.generateOas(services, "Proxy Service", "Genarated OAS Document", "0.1");
        oasDoc = GSONPretty.toJson(oas).getBytes();
    }

    public void stopClient() throws Exception {
        client.stop();
        stop();
    }

    private static HttpClient createHttpClient(ProxySettings settings) {
        HttpClient client = new HttpClient();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            settings.maxServerPoolSize(),
            settings.clientKeepAliveTimeMs(),
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new SimpleThreadFactory("client-threads", Thread.NORM_PRIORITY, true));
        threadPoolExecutor.prestartAllCoreThreads();
        client.setExecutor(new ExecutorThreadPool(threadPoolExecutor));
        client.setIdleTimeout(settings.clientKeepAliveTimeMs());
        client.setConnectTimeout(settings.connectTimeoutMs());

        return client;
    }

    @Override
    public void handle(String uri, final Request request, HttpServletRequest servletRequest,
                       HttpServletResponse servletResponse) throws IOException {

        if (uri.equals("/oas.json")) {
            servletResponse.getOutputStream().write(oasDoc);
            servletResponse.setContentLength(servletResponse.getBufferSize());
            servletResponse.setStatus(200);
        } else {
            // todo: translate and handle
        }



    }

}
