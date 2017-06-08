package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
public class DocumentationAdderTest {

    public static final String NS = "http://meeuw.org/a";

    @XmlDocumentation(value = "some docu about a", namespace = NS, name = "a")
    @XmlType(namespace = NS)
    public static class A {

        @XmlElement(namespace = NS)
        @XmlDocumentation(value = "some docu of element b in a")
        B b;
    }

    @XmlDocumentation(value = "docu about b", namespace = NS, name = "b")
    @XmlType(namespace = NS)
    public static class B {

    }


    @Test
    public void addDocumentation() throws JAXBException, IOException, SAXException, TransformerException, ParserConfigurationException {
        DocumentationAdder collector = new DocumentationAdder(A.class);
        StringWriter writer = new StringWriter();
        for (Map.Entry<String, Source> sourceEntry : schemaSources(A.class).entrySet()) {
            collector.transform(sourceEntry.getValue(), new StreamResult(writer));
        }
        assertThat(writer.toString()).isXmlEqualTo("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            " <xs:schema targetNamespace=\"http://meeuw.org/a\" version=\"1.0\"\n" +
            "    xmlns:tns=\"http://meeuw.org/a\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <xs:complexType name=\"a\">\n" +
            "        <xs:documentation>some docu about a</xs:documentation>\n" +
            "        <xs:sequence>\n" +
            "            <xs:element form=\"qualified\" minOccurs=\"0\" name=\"b\" type=\"tns:b\"/>\n" +
            "        </xs:sequence>\n" +
            "    </xs:complexType>\n" +
            "    <xs:complexType name=\"b\">\n" +
            "        <xs:documentation>docu about b</xs:documentation>\n" +
            "        <xs:sequence/>\n" +
            "    </xs:complexType>\n" +
            "</xs:schema>");

    }


    protected Map<String, Source> schemaSources(Class... classes) throws JAXBException, IOException, SAXException {
        JAXBContext context = JAXBContext.newInstance(classes);
        final Map<String, DOMResult> results = new HashMap<>();
        context.generateSchema(new SchemaOutputResolver() {
            @Override
            public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
                DOMResult dom = new DOMResult();
                dom.setSystemId(namespaceUri);
                results.put(namespaceUri, dom);
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

}
