package com.mcsuka.xml.proxy;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
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
    private static Semaphore waitTillShutdown = new Semaphore(1);

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

        } catch (Throwable t) {
            logger.error("Unexpected exception at startup", t);
        }
    }

    public RestToSoapProxyApp(Properties props) {

    }

    @Override
    public void run() {
        try {
            waitTillShutdown.acquire();
        } catch (InterruptedException ie) {
            logger.error("The main thread was interrupted", ie);
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

    void initialize() {
        waitTillShutdown.acquireUninterruptibly();

    }

    void terminate() {

        waitTillShutdown.release();
    }

    private static Server createServer(ProxySettings settings, Handler handler) {

        ConnectionFactory[] factories = new ConnectionFactory[]{
            new ProxyConnectionFactory(),
            new HttpConnectionFactory(new HttpConfiguration())
        };

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

        ServerConnector connector = new ServerConnector(server, factories);
        connector.setHost(settings.serverHost());
        connector.setPort(settings.serverPort());
        server.addConnector(connector);

        return server;
    }

}
