package org.meeuw.jaxbdocumentation;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Supplies a {@link Transformer} that addes xs:documentation tags to an existing XSD.
 * The contents of the documentation tags is determined by introspecting while looking for {@link @XmlDocumentation} tags.
 *
 * @author Michiel Meeuwissen
 * @since 0.1
 */
public class DocumentationAdder implements Supplier<Transformer> {

    private static final String URI_FOR_DOCUMENTATIONS = "http://meeuw.org/documentations";

    private final Class<?>[] classes;
    private Transformer transformer;

    public DocumentationAdder(Class<?>... classes) {
        this.classes = classes;
    }

    public void transform(Source source, StreamResult out) throws TransformerException {
        get().transform(source, out);
    }

    @Override
    public  Transformer get() {
        if (transformer == null) {
            try {
                TransformerFactory transFact = TransformerFactory.newInstance();
                transformer = transFact.newTransformer(new StreamSource(DocumentationAdder.class.getResourceAsStream("/add-documentation.xsd")));
                transformer.setURIResolver(new DocumentationResolver(createDocumentations(classes)));
            } catch (TransformerConfigurationException | ParserConfigurationException | SAXException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return transformer;
    }

    protected static Map<String, String> createDocumentations(Class<?>... classes) throws IOException, ParserConfigurationException, SAXException {
        Map<String, String> docs = new HashMap<>();
        Set<Object> handled = new HashSet<>();
        for (Class<?> clazz : classes) {
            put(clazz, docs, handled);
        }
        return docs;
    }
    private static void put(Class<?> clazz, Map<String, String> docs, Set<Object> handled) {
        if (handled.add(clazz)) {
            put(clazz.getAnnotation(XmlDocumentation.class), docs);
            for (Field field : clazz.getDeclaredFields()) {
                put(field.getAnnotation(XmlDocumentation.class), docs);
                put(field.getType(), docs, handled);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                put(method.getAnnotation(XmlDocumentation.class), docs);
                put(method.getReturnType(), docs, handled);

            }
        }
    }

    private static void put(XmlDocumentation annot, Map<String, String> docs) {
        if (annot != null) {
            docs.put(qname(annot).toString(), annot.value());
        }
    }

    private static QName qname(XmlDocumentation annot) {
        return new QName(annot.namespace(), annot.name());
    }

    /**
     * Represents a map of values as a StreamSource, which can be resolved in XSLT to a document.
     * (a node-set xslt parameter would have been a more logical idea, but the default xslt parser of the jvm's don't get that)
     *
     */
    private static StreamSource toDocument(Map<String, String> map) throws IOException, ParserConfigurationException, SAXException {
        Properties properties = new Properties();
        properties.putAll(map);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        properties.storeToXML(stream, null);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
        parser.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if ("http://java.sun.com/dtd/properties.dtd".equals(systemId)) {
                    InputStream input = getClass().getResourceAsStream("/java/util/properties.dtd");
                    return new InputSource(input);
                }
                return null;
            }
        });
        return new StreamSource(new ByteArrayInputStream(stream.toByteArray()));
    }


    private static class DocumentationResolver implements URIResolver {
        final Map<String, String> docs;

        private DocumentationResolver(Map<String, String> docs) {
            this.docs = docs;
        }

        @Override
        public Source resolve(String href, String base) throws TransformerException {
            if (URI_FOR_DOCUMENTATIONS.equals(href)) {
                try {
                    return DocumentationAdder.toDocument(docs);
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    throw new TransformerException(e);
                }
            } else {
                return null;
            }
        }
    }
}
