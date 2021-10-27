package org.meeuw.jaxbdocumentation;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.meeuw.xml.bind.annotation.XmlDocumentation;

/**
 * Supplies a {@link Transformer} that adds xs:documentation tags to an existing XSD.
 * The contents of the documentation tags is determined by introspecting while looking for {@link XmlDocumentation} tags.
 *
 * The transformer is an actual XSLT `add-documentation.xslt`, which combines the information collected via introspection with the original XSD XML.
 *
 * @author Michiel Meeuwissen
 * @since 0.1
 */
public class DocumentationAdder implements Supplier<Transformer> {

    private static final String URI_FOR_DOCUMENTATIONS = "http://meeuw.org/documentations";

    private static final String PARAM_XML_STYLESHEET = "xmlStyleSheet";
    private static final String PARAM_DEBUG = "debug";

    /**
     * This map caches per type, the known documentation annotations.
     */
    private static final Map<Class<?>[], Map<String, String>> CACHE = new ConcurrentHashMap<>();

    private final Class<?>[] classes;
    private Transformer transformer;
    @Getter
    @Setter
    private boolean useCache = false;
    @Setter
    @Getter
    private String xmlStyleSheet = null;
    @Getter
    @Setter
    private boolean debug = false;


    public DocumentationAdder(Class<?>... classes) {
        this.classes = classes;
    }

    public void transform(Source source, Result out) throws TransformerException {
        get().transform(source, out);
    }


    @Override
    public Transformer get() {
        if (transformer == null) {
            try {
                TransformerFactory transFact = TransformerFactory.newInstance();
                transformer = transFact.newTransformer(
                    new StreamSource(DocumentationAdder.class.getResourceAsStream("/add-documentation.xslt")));
                transformer.setURIResolver(new DocumentationResolver(createDocumentations(classes)));
                if (xmlStyleSheet != null) {
                    transformer.setParameter(PARAM_XML_STYLESHEET, this.xmlStyleSheet);
                }
                transformer.setParameter(PARAM_DEBUG, this.debug);
            } catch (TransformerConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return transformer;
    }


    public Class<?>[] getClasses() {
        return classes;
    }

    public void write(Writer writer) throws JAXBException, IOException, TransformerException {
        for (Map.Entry<String, Source> sourceEntry : Utils.schemaSources(getClasses()).entrySet()) {
            transform(sourceEntry.getValue(), new StreamResult(writer));
        }
    }

    public String write() throws JAXBException, IOException, TransformerException {
        StringWriter writer = new StringWriter();
        write(writer);
        return writer.toString();
    }

    protected Map<String, String> createDocumentations(Class<?>... classes) {
        Function<Class<?>[], Map<String, String>> creator = (cc) -> {
            CollectContext collectContext = new CollectContext();
            for (Class<?> clazz : cc) {
                handleClass(clazz, collectContext);
            }
            return collectContext.docs;
        };
        if (useCache) {
            return CACHE.computeIfAbsent(classes, creator);
        } else {
            return creator.apply(classes);
        }
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

            @NonNull String parent = handle(clazz.getAnnotation(XmlDocumentation.class), defaultName(clazz), collectContext.docs);
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

    private static void handleField(@NonNull String parent, Field field, XmlAccessType accessType, CollectContext collectContext) {

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


        String namespace = "{}";
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

    private static String defaultName(Method method) {
        String name = method.getName();
        if (name.startsWith("get")) {
            return name.substring(3);
        } else {
            return name;
        }
    }

    private static String handle(XmlDocumentation xmlDocumentation, String name, Map<String, String> docs) {
        if (xmlDocumentation != null) {
            docs.put(name, xmlDocumentation.value());
        }

        return name;
    }

    private static String handle(Annotation[] annots, @NonNull String parent, String name, boolean implicit, Map<String, String> docs) {
        XmlDocumentation annot = null;
        String type = "ELEMENT";
        boolean explicit = false;
        List<String> extraNames = new ArrayList<>();
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
                for (XmlElement e : ((XmlElements) a).value()) {
                    if (! e.name().equals("##default")) {
                        extraNames.add(e.name());
                    }
                }
            }
        }
        if (annot != null && (implicit || explicit)) {
            String result  = name(annot, parent, type, name);
            docs.put(result, annot.value());
            for (String extraName : extraNames) {
                docs.put(name(annot, parent, "ELEMENT", extraName), annot.value());
            }
            return result;
        }
        return null;
    }

    private static String name(
        @NonNull XmlDocumentation annot,
        @NonNull String parent,
        @NonNull String type,
        @NonNull String name) {
        return parent + "|" + type + "|" + name;
    }

    /**
     * Represents a map of values as a StreamSource, which can be resolved in XSLT to a document.
     * (a node-set xslt parameter would have been a more logical idea, but the default xslt parser of the jvm's don't get that)
     *
     */
    private static StreamSource toDocument(Map<String, String> map) throws IOException {
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
                } catch (IOException e) {
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
