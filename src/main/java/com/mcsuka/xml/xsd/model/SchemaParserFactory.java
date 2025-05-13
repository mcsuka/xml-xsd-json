package com.mcsuka.xml.xsd.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mcsuka.xml.xsd.tools.DocumentSource;
import com.mcsuka.xml.xsd.tools.DocumentSourceException;

/**
 * Initialize a SchemaParser.
 */
public class SchemaParserFactory {

    private static final Logger logger = LoggerFactory.getLogger(SchemaParserFactory.class);
    private static final ConcurrentHashMap<String, SchemaParser> modelCache = new ConcurrentHashMap<>();

    /**
     * Create a new SchemaParser or take it from the cache, if it was already initialized. SchemaParsers must be uniquely identified by their xsdLocation.
     */
    public synchronized static SchemaParser newSchemaParser(String xsdLocation, DocumentSource docSource) throws XPathExpressionException, DocumentSourceException {
        String url = normalizeUrl(xsdLocation);
        SchemaParser model = modelCache.get(url);
        if (model == null) {
            model = new SchemaParser(url, docSource);
            SchemaParser oldModel = modelCache.putIfAbsent(url, model);
            if (oldModel != null) {
                return oldModel;
            }
        } else {
            logger.debug("XSD model found in cache: " + url);
        }
        return model;
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
        logger.debug("Received URL: " + url);
        String[] urlParts = url.replace('\\', '/').split("/", -1);
        List<String> newUrlParts = Arrays.stream(urlParts)
                .collect(ArrayList::new,
                        (c, e) -> {
                            if (".".equals(e)) {
                                // do nothing
                            } else if ("..".equals(e)) {
                                if (!c.isEmpty()) c.removeLast();
                            } else {
                                c.add(e);
                            }
                        },
                        List::addAll
                );
        return String.join("/", newUrlParts);
    }

}
