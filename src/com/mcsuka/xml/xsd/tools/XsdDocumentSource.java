package com.mcsuka.xml.xsd.tools;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class XsdDocumentSource implements DocumentSource {

    private final HashMap<String, String> pfxMap = new HashMap<>();
    private final Charset charset;

    public XsdDocumentSource(String charsetName) {
        charset = Charset.forName(charsetName);
    }
    
    public XsdDocumentSource() {
        charset = Charset.forName("UTF-8");
    }
    
    @Override
    public Document parse(String url) throws DocumentSourceException {
        try {
            InputStreamReader inputStreamReader = null;
            if (url.startsWith("http://") || url.startsWith("https://")) {
                URL myUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) myUrl.openConnection();
                conn.setRequestMethod("GET");
                inputStreamReader = new InputStreamReader(conn.getInputStream(), charset);
            } else {
                String fileName = url.startsWith("file://") ? url.substring(7) : url;
                inputStreamReader = new InputStreamReader(new FileInputStream(fileName), charset);
            }
            Document doc = XmlTools.getDocumentBuilder().parse(new InputSource(inputStreamReader));
            inputStreamReader.close();
            return doc;
        } catch (Throwable t) {
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
