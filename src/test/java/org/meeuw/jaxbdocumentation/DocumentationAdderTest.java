package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
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

        @XmlAttribute
        @XmlDocumentation(value = "documentation of attribute")
        String attr;

        @XmlAttribute(name = "int")
        @XmlDocumentation(value = "documentation of attribute integer")
        Integer intAttribute;

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

        for (Map.Entry<String, Source> sourceEntry : collector.schemaSources().entrySet()) {
            collector.transform(sourceEntry.getValue(), new StreamResult(writer));
        }
        assertThat(writer.toString()).isXmlEqualTo("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://meeuw.org/a\" version=\"1.0\"\n" +
            "    xmlns:tns=\"http://meeuw.org/a\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <xs:complexType name=\"a\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>some docu about a</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:sequence>\n" +
            "            <xs:element form=\"qualified\" minOccurs=\"0\" name=\"b\" type=\"tns:b\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>some docu of element b in a</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "        </xs:sequence>\n" +
            "        <xs:attribute name=\"attr\" type=\"xs:string\">\n" +
            "            <xs:annotation>\n" +
            "                <xs:documentation>documentation of attribute</xs:documentation>\n" +
            "            </xs:annotation>\n" +
            "        </xs:attribute>\n" +
            "        <xs:attribute name=\"int\" type=\"xs:int\">\n" +
            "            <xs:annotation>\n" +
            "                <xs:documentation>documentation of attribute integer</xs:documentation>\n" +
            "            </xs:annotation>\n" +
            "        </xs:attribute>\n" +
            "    </xs:complexType>\n" +
            "    <xs:complexType name=\"b\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>docu about b</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:sequence/>\n" +
            "    </xs:complexType>\n" +
            "</xs:schema>");

    }




}
