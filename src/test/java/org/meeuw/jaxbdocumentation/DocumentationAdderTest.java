package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.meeuw.xml.bind.annotation.XmlDocumentation;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
public class DocumentationAdderTest {

    public static final String NS = "http://meeuw.org/a";

    @XmlTransient
    public static class Parent {
        @XmlAttribute
        @XmlDocumentation(value = "documentation of parent attribute")
        String parrentAttr;

    }

    @XmlDocumentation(value = "some docu about a")
    @XmlType(namespace = NS)
    public static class A extends Parent {

        @XmlAttribute
        @XmlDocumentation(value = "documentation of attribute")
        String attr;

        @XmlAttribute(name = "int")
        @XmlDocumentation(value = "documentation of attribute integer")
        Integer intAttribute;

        @XmlElement(namespace = NS)
        @XmlDocumentation(value = "some docu of element b in a")
        B b;

        @XmlElement
        @XmlDocumentation("Documentation for enum element")
        SomeEnum someEnum;

        @XmlElements(
            @XmlElement(type = C.class)
        )
        @XmlDocumentation(value = "some docu about this list")
        List<Object> list;

        @XmlTransient
        public String getAttr() {
            return attr;
        }

        public void setAttr(String attr) {
            this.attr = attr;
        }

        @XmlTransient
        public Integer getIntAttribute() {
            return intAttribute;
        }

        public void setIntAttribute(Integer intAttribute) {
            this.intAttribute = intAttribute;
        }
    }

    @XmlDocumentation(value = "docu about b", namespace = NS, name = "b")
    @XmlType(namespace = NS)
    public static class B {

    }

    @XmlDocumentation(value = "docu about c")
    @XmlType(namespace = NS)
    public static class C {

    }

    @XmlType(namespace = NS)
    @XmlDocumentation("Documentation about some enum")
    public enum SomeEnum {
        x,
        @XmlDocumentation("documentation for enum value")
        y,
        z

    }

    @Test
    public void addDocumentation() throws JAXBException, IOException, SAXException, TransformerException, ParserConfigurationException {
        DocumentationAdder collector = new DocumentationAdder(A.class);
        StringWriter writer = new StringWriter();

        for (Map.Entry<String, Source> sourceEntry : collector.schemaSources().entrySet()) {
            collector.transform(sourceEntry.getValue(), new StreamResult(writer));
        }
        assertThat(writer.toString()).isXmlEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "            <xs:element minOccurs=\"0\" name=\"someEnum\" type=\"tns:someEnum\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>Documentation for enum element</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "            <xs:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"list\" type=\"tns:c\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>some docu about this list</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "        </xs:sequence>\n" +
            "        <xs:attribute name=\"parrentAttr\" type=\"xs:string\">\n" +
            "            <xs:annotation>\n" +
            "                <xs:documentation>documentation of parent attribute</xs:documentation>\n" +
            "            </xs:annotation>\n" +
            "        </xs:attribute>\n" +
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
            "    <xs:complexType name=\"c\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>docu about c</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:sequence/>\n" +
            "    </xs:complexType>\n" +
            "    <xs:simpleType name=\"someEnum\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>Documentation about some enum</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:enumeration value=\"x\"/>\n" +
            "            <xs:enumeration value=\"y\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>documentation for enum value</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:enumeration>\n" +
            "            <xs:enumeration value=\"z\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "</xs:schema>");

    }


    @XmlType(namespace = NS)
    public static class EnumValueTest {
        @XmlAttribute
        SomeEnum someEnum;
    }
    @Test
    public void enumValue() throws JAXBException, IOException, SAXException, TransformerException {
        DocumentationAdder collector = new DocumentationAdder(EnumValueTest.class);
        StringWriter writer = new StringWriter();

        for (Map.Entry<String, Source> sourceEntry : collector.schemaSources().entrySet()) {
            collector.transform(sourceEntry.getValue(), new StreamResult(writer));
        }
        assertThat(writer.toString()).isXmlEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://meeuw.org/a\" version=\"1.0\"\n" +
            "    xmlns:tns=\"http://meeuw.org/a\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <xs:complexType name=\"enumValueTest\">\n" +
            "        <xs:sequence/>\n" +
            "        <xs:attribute name=\"someEnum\" type=\"tns:someEnum\"/>\n" +
            "    </xs:complexType>\n" +
            "    <xs:simpleType name=\"someEnum\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>Documentation about some enum</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:restriction base=\"xs:string\">\n" +
            "            <xs:enumeration value=\"x\"/>\n" +
            "            <xs:enumeration value=\"y\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>documentation for enum value</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:enumeration>\n" +
            "            <xs:enumeration value=\"z\"/>\n" +
            "        </xs:restriction>\n" +
            "    </xs:simpleType>\n" +
            "</xs:schema>");
    }




}
