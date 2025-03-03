package com.mcsuka.xml.xsd.tools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class XsdDocumentSource implements DocumentSource {

    private static final Logger logger = LoggerFactory.getLogger(XsdDocumentSource.class);
    private final HashMap<String, String> pfxMap = new HashMap<>();
    private final Charset charset;

    public XsdDocumentSource(String charsetName) {
        charset = Charset.forName(charsetName);
    }

    public XsdDocumentSource() {
        charset = StandardCharsets.UTF_8;
    }

    @Override
    public Document parse(String url) throws DocumentSourceException {
        try {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                URL myUrl = new URI(url).toURL();
                HttpURLConnection conn = (HttpURLConnection) myUrl.openConnection();
                conn.setRequestMethod("GET");
                return inputStreamToDoc(conn::getInputStream, charset);
            } else {
                String fileName = url.startsWith("file://") ? url.substring(7) : url;
                return inputStreamToDoc(() -> new FileInputStream(fileName), charset);
            }
        } catch (Throwable t) {
            logger.warn("Error reading XSD from URL " + url, t);
            throw new DocumentSourceException("Unable to parse XSD on url " + url, t);
        }
    }

    /**
     * Dummy implementation
     */
    public Map<String, String> getPrefixMap() {
        return pfxMap;
    }

}
