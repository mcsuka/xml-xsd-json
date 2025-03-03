package com.mcsuka.xml.xsd.tools;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Callable;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
    Document parse(String url) throws DocumentSourceException;

    /**
     * @return namespace prefix -> namespace map
     */
    Map<String, String> getPrefixMap();



    default Document inputStreamToDoc(Callable<InputStream> streamSupplier, Charset charset) throws Exception  {
        try (InputStream stream = streamSupplier.call()) {
            try (InputStreamReader reader = new InputStreamReader(stream, charset)) {
                return XmlTools.getDocumentBuilder().parse(new InputSource(reader));
            }
        }
    }


}
