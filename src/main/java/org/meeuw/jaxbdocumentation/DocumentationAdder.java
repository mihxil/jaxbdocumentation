package org.meeuw.jaxbdocumentation;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
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
    private File tempFile;

    public DocumentationAdder(Class<?>... classes) {
        this.classes = classes;
    }

    public void transform(Source source, Result out) throws TransformerException {
        get().transform(source, out);
    }

    /**
     * Returns the associated XSD schema's as a map of {@link Source}'s. The key is the namespace.
     */
    public Map<String, Source> schemaSources() throws JAXBException, IOException, SAXException {
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

    public Map<String, Source> transformedSources() throws JAXBException, IOException, SAXException, TransformerException {
        Map<String, Source> result = new HashMap<>();
        for (Map.Entry<String, Source> sourceEntry : schemaSources().entrySet()) {
            DOMResult domResult = new DOMResult();
            transform(sourceEntry.getValue(), domResult);
            result.put(sourceEntry.getKey(), new DOMSource(domResult.getNode()));
        }
        return result;
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
        if (clazz.isPrimitive()) {
            return;
        }
        if (Number.class.isAssignableFrom(clazz)) {
            return;
        }
        if (String.class.isAssignableFrom(clazz)) {
            return;
        }
        if (handled.add(clazz)) {
            String parent = put(clazz.getAnnotations(), null, defaultName(clazz), docs);
            for (Field field : clazz.getDeclaredFields()) {
                put(field.getAnnotations(), parent, defaultName(field), docs);
                put(field.getType(), docs, handled);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                put(method.getAnnotations(), parent, defaultName(method), docs);
                put(method.getReturnType(), docs, handled);
            }
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && superClass.getAnnotation(XmlTransient.class) != null) {
                for (Field field : superClass.getDeclaredFields()) {
                    put(field.getAnnotations(), parent, defaultName(field), docs);
                    put(field.getType(), docs, handled);
                }
                for (Method method : superClass.getDeclaredMethods()) {
                    put(method.getAnnotations(), parent, defaultName(method), docs);
                    put(method.getReturnType(), docs, handled);
                }
                superClass = superClass.getSuperclass();

            }

        }
    }
    private static String defaultName(Class<?> clazz) {
        return clazz.getSimpleName();
    }

    private static String defaultName(Field field) {
        return field.getName();
    }
    private static String defaultName(Method method) {
        String name = method.getName();
        if (name.startsWith("get")) {
            return name.substring(3);
        } else {
            return name;
        }
    }

    private static String put(Annotation[] annots, String parent, String name, Map<String, String> docs) {
        XmlDocumentation annot = null;
        String type = "ELEMENT";
        for (Annotation a : annots) {
            if (a instanceof XmlDocumentation) {
                annot = (XmlDocumentation) a;
            }
            if (a instanceof XmlAttribute) {
                type = "ATTRIBUTE";
                if (!((XmlAttribute) a).name().equals("##default")) {
                    name = ((XmlAttribute) a).name();
                }
            }
            if (a instanceof XmlElement) {
                if (!((XmlElement) a).name().equals("##default")) {
                    name = ((XmlElement) a).name();
                }
            }
        }
        if (annot != null) {
            String result  = name(annot, parent, type, name);
            docs.put(result, annot.value());
            return result;
        } else {
            return name;
        }

    }

    private static String name(XmlDocumentation annot, String parent, String type, String defaultName) {
        String name =  annot.name().isEmpty() ? defaultName : annot.name();
        String namespace = annot.namespace().isEmpty() ? "" : "{" + annot.namespace() + "}";
        return (parent == null ? "" : (parent + "|" + type + "|") ) + namespace + name;
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
