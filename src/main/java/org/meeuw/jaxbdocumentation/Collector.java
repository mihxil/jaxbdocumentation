package org.meeuw.jaxbdocumentation;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
public class Collector {

	private final Class<?>[] classes;
	private Transformer transformer;

	public Collector(Class<?>... classes) {
		this.classes = classes;
	}

	public void transform(Source source, StreamResult out) throws TransformerException {
		getTransformer().transform(source, out);
	}

	public  Transformer getTransformer() {
		if (transformer == null) {
			try {
				TransformerFactory transFact = TransformerFactory.newInstance();
				transformer = transFact.newTransformer(new StreamSource(Collector.class.getResourceAsStream("/add-documentation.xsd")));
				transformer.setURIResolver(new DocumentationResolver(createDocumentations(classes)));
			} catch (TransformerConfigurationException | ParserConfigurationException | SAXException | IOException e) {
				throw new RuntimeException(e);
			}
		}
		return transformer;
	}

	public static void transform(Map<String, String> docs, Source source, OutputStream out) throws TransformerException {
		TransformerFactory transFact = TransformerFactory.newInstance();
		Transformer trans = transFact.newTransformer(new StreamSource(Collector.class.getResourceAsStream("/add-documentation.xsd")));
		trans.setURIResolver(new DocumentationResolver(docs));
		trans.transform(source, new StreamResult(out));
	}

	public static Map<String, String> createDocumentations(Class<?>... classes) throws IOException, ParserConfigurationException, SAXException {
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
			docs.put(annot.qname(), annot.value());
		}
	}

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
			if ("http://meeuw.org/documentations".equals(href)) {
				try {
					return Collector.toDocument(docs);
				} catch (IOException | ParserConfigurationException | SAXException e) {
					throw new TransformerException(e);
				}
			} else {
				return null;
			}
		}
	}
}
