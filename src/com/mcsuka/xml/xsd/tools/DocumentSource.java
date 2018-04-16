package com.mcsuka.xml.xsd.tools;

import java.util.Map;

import org.w3c.dom.Document;

/**
 * Generic XML document loader. Implementations may load and parse XML documents
 * from any source (File, FTP, HTTP, ...)
 */
public interface DocumentSource {

    /**
     * Converts an XML text into a DOM document.
     * 
     * @param url
     *            Points to the XML text. Format of the URL depends on the
     *            implementation.
     * @return DOM representation of the XML
     */
    public Document parse(String url) throws DocumentSourceException;

    /**
     * @return namespace prefix -> namespace map
     */
    public Map<String, String> getPrefixMap();

}
