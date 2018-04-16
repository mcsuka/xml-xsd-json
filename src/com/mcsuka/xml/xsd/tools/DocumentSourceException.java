package com.mcsuka.xml.xsd.tools;

public class DocumentSourceException extends Exception {

    /**
     * Exception thrown while trying to load an XML document from a document source.
     */
    private static final long serialVersionUID = 2917834721L;

    public DocumentSourceException(String description, Throwable cause) {
        super(description, cause);
    }
}
