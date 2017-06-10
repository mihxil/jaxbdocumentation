package org.meeuw.jaxbdocumentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.meeuw.xml.bind.annotation.XmlDocumentation;
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
        CollectContext collectContext = new CollectContext();
        for (Class<?> clazz : classes) {
            handleClass(clazz, collectContext);
        }
        return collectContext.docs;
    }
    private static void handleClass(Class<?> clazz, CollectContext collectContext) {
        if (clazz.isPrimitive()) {
            return;
        }
        if (clazz.getPackage() != null && clazz.getPackage().getName().startsWith("java.")) {
            return;
        }
        if (collectContext.handled.add(clazz)) {

            XmlAccessType accessType = getAccessType(clazz);

            String parent = handle(clazz.getAnnotations(), null, defaultName(clazz), true, collectContext.docs);
            for (Field field : clazz.getDeclaredFields()) {
                handleField(parent, field, accessType, collectContext);
            }
            for (Method method : clazz.getDeclaredMethods()) {
                handleMethod(parent, method, accessType, collectContext);
            }
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && superClass.getAnnotation(XmlTransient.class) != null) {
                XmlAccessType superAccessType = getAccessType(superClass);
                for (Field field : superClass.getDeclaredFields()) {
                    handleField(parent, field, superAccessType, collectContext);
                }
                for (Method method : superClass.getDeclaredMethods()) {
                    handleMethod(parent, method, superAccessType, collectContext);
                }
                superClass = superClass.getSuperclass();
            }

        }
    }
    private static XmlAccessType getAccessType(Class<?> clazz) {
        XmlAccessorType accessorType = clazz.getAnnotation(XmlAccessorType.class);
        return accessorType == null ? XmlAccessType.PUBLIC_MEMBER : accessorType.value();
    }
    private static void handleField(String parent, Field field, XmlAccessType accessType, CollectContext collectContext) {

        if (Modifier.isStatic(field.getModifiers())) {
            if (Enum.class.isAssignableFrom(field.getType())) {
                handleEnumValue(parent, field, collectContext);
            }
            return;
        }
        String defaultFieldName = field.getName();
        boolean implicit = false;
        switch (accessType) {
            case PUBLIC_MEMBER:
                if (!Modifier.isPublic(field.getModifiers())) {
                    break;
                }
            case FIELD:
                implicit = true;
                break;
        }
        handle(field.getAnnotations(), parent, defaultFieldName, implicit, collectContext.docs);
        recurseXmlElementAnnotations(
            collectXmlElements(field.getAnnotation(XmlElement.class), field.getAnnotation(XmlElements.class)),
            collectContext
        );
        // recurse
        handleClass(field.getType(), collectContext);
    }

    private static void handleEnumValue(String parent, Field field, CollectContext collectContext) {
        String defaultFieldName = field.getName();
        XmlDocumentation annot = field.getAnnotation(XmlDocumentation.class);
        if (annot != null) {
            String key = name(annot, parent, "ENUMERATION", defaultFieldName);
            collectContext.docs.put(key, annot.value());
        }

    }

    private static void recurseXmlElementAnnotations(Collection<XmlElement> xmlElements, CollectContext collectContext) {
        for (XmlElement xmlElement : xmlElements) {
            if (xmlElement.type() != XmlElement.DEFAULT.class) {
                handleClass(xmlElement.type(), collectContext);
            }
        }
    }
    private static List<XmlElement> collectXmlElements(XmlElement xmlElement, XmlElements xmlElements) {
        List<XmlElement> result = new ArrayList<>();
        if (xmlElement != null) {
            result.add(xmlElement);
        }
        if (xmlElements != null) {
            result.addAll(Arrays.asList(xmlElements.value()));
        }
        return result;
    }

    private static void handleMethod(String parent, Method method, XmlAccessType accessType, CollectContext collectContext) {
        if (Modifier.isStatic(method.getModifiers())) {
            return;
        }
        boolean implicit = false;
        switch (accessType) {
            case PUBLIC_MEMBER:
                if (!Modifier.isPublic(method.getModifiers())) {
                    break;
                }
            case PROPERTY:
                implicit = true;
                break;
        }
        String defaultFieldName = defaultName(method);

        handle(method.getAnnotations(), parent, defaultFieldName, implicit, collectContext.docs);
        recurseXmlElementAnnotations(
            collectXmlElements(method.getAnnotation(XmlElement.class), method.getAnnotation(XmlElements.class)),
            collectContext
        );
        // recurse
        handleClass(method.getReturnType(), collectContext);
    }

    private static String defaultName(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        String name = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);


        String namespace = "";
        Package pack = clazz.getPackage();
        if (pack != null) {
            XmlSchema schema = pack.getAnnotation(XmlSchema.class);
            if (schema != null && !"##default".equals(schema.namespace())) {
                namespace = "{" + schema.namespace() + "}";
            }
        }

        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation instanceof XmlType) {
                XmlType xmlType = (XmlType) annotation;
                if (!"##default".equals(xmlType.namespace())) {
                    namespace = "{" + xmlType.namespace() + "}";
                }
                if (!"##default".equals(xmlType.name())) {
                    name = xmlType.name();
                }
            }
        }
        return namespace + name;
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

    private static String handle(Annotation[] annots, String parent, String name, boolean implicit, Map<String, String> docs) {
        XmlDocumentation annot = null;
        String type = "ELEMENT";
        boolean explicit = false;
        for (Annotation a : annots) {
            if (a instanceof XmlDocumentation) {
                annot = (XmlDocumentation) a;
            }
            if (a instanceof XmlTransient) {
                return null;
            }
            if (a instanceof XmlAttribute) {
                explicit = true;
                type = "ATTRIBUTE";
                if (!((XmlAttribute) a).name().equals("##default")) {
                    name = ((XmlAttribute) a).name();
                }
            }
            if (a instanceof XmlElement) {
                explicit = true;
                if (!((XmlElement) a).name().equals("##default")) {
                    name = ((XmlElement) a).name();
                }
            }
            if (a instanceof XmlElements) {
                explicit = true;
            }
        }
        if (annot != null && (implicit || explicit)) {
            String result  = name(annot, parent, type, name);
            docs.put(result, annot.value());
            return result;
        }
        return null;
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
        StreamSource source = new StreamSource(new ByteArrayInputStream(stream.toByteArray()));
        //source.setSystemId(URI_FOR_DOCUMENTATIONS);
        return source;
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
    private static class CollectContext {
        final Map<String, String> docs = new HashMap<>();
        final Set<Object> handled = new HashSet<>();
    }
}
