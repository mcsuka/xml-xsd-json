package com.mcsuka.xml.proxy;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RestToSoapProxyApp extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(RestToSoapProxyApp.class);
    private static final Semaphore waitTillShutdown = new Semaphore(1);

    private final ProxySettings settings;
    private ClientHandler clientHandler;
    private HttpClient httpClient;
    private Server server;
    private WebAppContext swaggerUI;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(
                "Usage: java com.mcsuka.xml.proxy.RestToSoapProxyApp <application.properties>");
            System.exit(-1);
        }
        try {
            Properties props = new Properties();
            props.load(new FileReader(args[0]));

            RestToSoapProxyApp app = new RestToSoapProxyApp(props);
            app.initialize();
            app.addShutdownHook();
            app.start();
            logger.info("ProxyApp has started");

        } catch (Throwable t) {
            logger.error("Unexpected exception at startup", t);
        }
    }

    public RestToSoapProxyApp(Properties props) throws Exception {
        settings = ProxySettings.propsToSettings(props);
    }

    @Override
    public void run() {
        try {
            waitTillShutdown.acquire();
        } catch (InterruptedException ie) {
            logger.error("The main thread was interrupted", ie);
        }
    }

    @Override
    public void start() {
        waitTillShutdown.acquireUninterruptibly();
        super.start();

        try {
            httpClient.start();
            clientHandler.start();
            swaggerUI.start();
            server.start();

        } catch (Exception e) {
            logger.error("Unexcepted exception at startup", e);
        }
    }

    private void addShutdownHook() {
        final RestToSoapProxyApp app = this;
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {

            @Override
            public void run() {
                try {
                    app.terminate();
                } catch (Exception e) {
                    logger.error("Unexcepted exception at shutdown", e);
                }
            }
        });
    }

    void initialize() throws Exception {
        swaggerUI = createWebApp();
        httpClient = createHttpClient(settings);
        clientHandler = new ClientHandler(settings, httpClient, swaggerUI);
        server = createServer(settings, clientHandler);
        swaggerUI.setServer(server);
    }

    void terminate() throws Exception {
        logger.info("ProxyApp is stopping");

        httpClient.stop();
        clientHandler.stop();
        server.stop();

        waitTillShutdown.release();
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

    private static Server createServer(ProxySettings settings, Handler handler) {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            2,
            settings.maxServerPoolSize(),
            settings.serverKeepAliveTimeMs(),
            TimeUnit.MILLISECONDS,
            new SynchronousQueue<>(),
            new SimpleThreadFactory("server-threads", Thread.NORM_PRIORITY, true));

        threadPoolExecutor.prestartAllCoreThreads();
        ExecutorThreadPool serverThreadPool = new ExecutorThreadPool(threadPoolExecutor);
        Server server = new Server(serverThreadPool);
        server.setHandler(handler);

        ConnectionFactory[] factories = new ConnectionFactory[]{
            new ProxyConnectionFactory(),
            new HttpConnectionFactory(new HttpConfiguration())
        };

        ServerConnector connector = new ServerConnector(server, factories);
        connector.setHost(settings.serverHost());
        connector.setPort(settings.serverPort());
        server.addConnector(connector);

        return server;
    }

    private WebAppContext createWebApp() {
        WebAppContext context = new WebAppContext();
        context.setContextPath("/swagger");
        context.setResourceBase(ClassLoader.getSystemResource("swagger").toExternalForm());
//        context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
        return context;
    }

}
