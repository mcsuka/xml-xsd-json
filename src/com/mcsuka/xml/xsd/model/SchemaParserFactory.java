package com.mcsuka.xml.xsd.model;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.xml.xpath.XPathExpressionException;

import com.mcsuka.xml.xsd.tools.DocumentSource;
import com.mcsuka.xml.xsd.tools.DocumentSourceException;

/**
 * Initialize a SchemaParser.
 */
public class SchemaParserFactory {

    private static final Logger logger = Logger.getLogger(SchemaParser.class.getName());
    private static ConcurrentHashMap<String, SchemaParser> modelCache = new ConcurrentHashMap<>();

    /**
     * Create a new SchemaParser or take it from the cache, if it was already initialized. SchemaParsers must be uniquely identified by their xsdLocation.
     */
    public synchronized static SchemaParser newSchemaParser(String xsdLocation, DocumentSource docSource) throws XPathExpressionException, DocumentSourceException {
        String url = normalizeUrl(xsdLocation);
        SchemaParser model = modelCache.get(url);
        if (model == null) {
            model = new SchemaParser(url, docSource);
            return modelCache.putIfAbsent(url, model);
        } else {
            logger.fine("XSD model found in cache: " + url);
            return model;
        }
    }

    /**
     * Clear the SchemaParser cache
     */
    public static void clearCache() {
        modelCache.clear();
    }

    /**
     * Replace reverse slash with forward slash. Try to resolve '..' references to named references.
     */
    public static String normalizeUrl(String url) {
        logger.fine("Received URL: " + url);
        String[] urlParts = url.replace('\\', '/').split("/");
        ArrayList<String> newUrlParts = new ArrayList<String>();
        for (String part : urlParts) {
            if (".".equals(part)) {
                // do nothing, do not add the part to the URL
            } else if ("..".equals(part)) {
                // remove last part
                int newLen = newUrlParts.size();
                if (newLen == 0) {
                    throw new RuntimeException("Unable to normalize the following URL: " + url);
                }
                newUrlParts.remove(newLen - 1);
            } else {
                newUrlParts.add(part);
            }
        }
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (String part : newUrlParts) {
            if (idx == 0) {
                sb.append(part);
            } else {
                sb.append("/" + part);
            }
            idx++;
        }
        return sb.toString();
    }

}
