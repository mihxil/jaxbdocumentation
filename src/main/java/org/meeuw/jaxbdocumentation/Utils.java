package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.SchemaOutputResolver;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.xml.sax.SAXException;

/**
 * @author Michiel Meeuwissen
 * @since 0.3
 */
public class Utils {

    /**
     * Returns XSD schema's as a map of {@link Source}'s. The key is the namespace.
     * @param classes To create schema's for.
     * @return a map with {@link Source}s
     * @throws JAXBException If something wrong with jaxb
     * @throws IOException if io
     */
    public static Map<String, Source> schemaSources(Class<?>... classes) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(classes);
        final Map<String, DOMResult> results = new HashMap<>();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) {
                DOMResult dom = new DOMResult();
                if (namespaceUri != null && namespaceUri.length() > 0) {
                    dom.setSystemId(namespaceUri);
                    results.put(namespaceUri, dom);
                } else {
                    dom.setSystemId(suggestedFileName);
                    results.put(suggestedFileName, dom);
                }
                return dom;
            }
        });
        Map<String, Source> sources = new HashMap<>();
        for (Map.Entry<String, DOMResult> result : results.entrySet()) {
            Source source = new DOMSource(result.getValue().getNode());
            sources.put(result.getKey(), source);
        }

        return sources;
    }

    public static Map<String, Source> documentationSchemaSources(Class<?>... classes) throws JAXBException, IOException, SAXException, TransformerException {
        DocumentationAdder transformer = new DocumentationAdder(classes);
        transformer.setUseCache(true);
        Map<String, Source> result = new HashMap<>();
        for (Map.Entry<String, Source> sourceEntry : schemaSources(transformer.getClasses()).entrySet()) {
            DOMResult domResult = new DOMResult();
            transformer.transform(sourceEntry.getValue(), domResult);
            result.put(sourceEntry.getKey(), new DOMSource(domResult.getNode()));
        }
        return result;
    }

}
