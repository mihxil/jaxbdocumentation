package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
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
     */
    public static Map<String, Source> schemaSources(Class<?>... classes) throws JAXBException, IOException, SAXException {
        JAXBContext context = JAXBContext.newInstance(classes);
        final Map<String, DOMResult> results = new HashMap<>();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
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
