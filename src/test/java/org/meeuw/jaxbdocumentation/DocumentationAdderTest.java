package org.meeuw.jaxbdocumentation;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.*;
import lombok.Data;

import java.io.IOException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;

import org.meeuw.xml.bind.annotation.XmlDocumentation;

import static org.xmlunit.assertj3.XmlAssert.assertThat;


/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@SuppressWarnings("unused")
public class DocumentationAdderTest {

    public static final String NS = "http://meeuw.org/a";

    @XmlTransient
    public static class Parent {
        @XmlAttribute
        @XmlDocumentation(value = "documentation of parent attribute")
        String parentAttr;

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

        @XmlElements({
            @XmlElement(type = C.class, name = "element")
        }
        )
        @XmlDocumentation(value = "some docu about this list")
        List<Object> elements;

        @XmlElement
        @XmlDocumentation("Documentation for lombok element")
        WithLombok withLombok;

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

    @XmlDocumentation(value = "docu about b")
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

    @XmlType(namespace = NS)
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlDocumentation("Documentation about some class annotated with lombok")
    @Data
    public static class WithLombok {
        @XmlDocumentation("about a")
        String value;
    }


    @Test
    public void addDocumentation() throws JAXBException, IOException, TransformerException, jakarta.xml.bind.JAXBException {
        DocumentationAdder collector = new DocumentationAdder(A.class);
        //collector.setXmlStyleSheet("xs3p.xsl")
        collector.setDebug(true);
        String string = collector.write();

        assertThat(string).and("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "            <xs:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"element\" type=\"tns:c\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>some docu about this list</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "            <xs:element minOccurs=\"0\" name=\"withLombok\" type=\"tns:withLombok\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>Documentation for lombok element</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "        </xs:sequence>\n" +
            "        <xs:attribute name=\"parentAttr\" type=\"xs:string\">\n" +
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
            "    <xs:complexType name=\"withLombok\">\n" +
            "        <xs:annotation>\n" +
            "            <xs:documentation>Documentation about some class annotated with lombok</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "        <xs:sequence>\n" +
            "            <xs:element minOccurs=\"0\" name=\"value\" type=\"xs:string\">\n" +
            "                <xs:annotation>\n" +
            "                    <xs:documentation>about a</xs:documentation>\n" +
            "                </xs:annotation>\n" +
            "            </xs:element>\n" +
            "        </xs:sequence>\n" +
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
            "</xs:schema>")
            .ignoreWhitespace()
            .ignoreComments()
            .areSimilar();
    }


    @XmlType(namespace = NS)
    public static class EnumValueTest {
        @XmlAttribute
        SomeEnum someEnum;
    }
    @Test
    public void enumValue() throws JAXBException, IOException, TransformerException {
        DocumentationAdder collector = new DocumentationAdder(EnumValueTest.class);
        String string  = collector.write();

        assertThat(string)
            .and("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "</xs:schema>").ignoreWhitespace().areSimilar();
    }

    @XmlDocumentation("with xml element")
    static class WithXmlElementName {
        @XmlDocumentation(value = "some docu about this list")
        @XmlElement(name = "element")
        List<String> elements;
    }

    @Test
    public void xmlElementName() throws JAXBException, IOException, TransformerException {
        DocumentationAdder adder = new DocumentationAdder(WithXmlElementName.class);
        adder.setDebug(true);
        String string = adder.write();

        assertThat(string).and("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\n" +
            "  <xs:complexType name=\"withXmlElementName\">\n" +
            "    <!--documentation key: {}withXmlElementName-->\n" +
            "    <xs:annotation>\n" +
            "      <xs:documentation>with xml element</xs:documentation>\n" +
            "    </xs:annotation>\n" +
            "    <xs:sequence>\n" +
            "      <xs:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"element\" type=\"xs:string\">\n" +
            "        <!--documentation key: {}withXmlElementName|ELEMENT|element-->\n" +
            "        <xs:annotation>\n" +
            "          <xs:documentation>some docu about this list</xs:documentation>\n" +
            "        </xs:annotation>\n" +
            "      </xs:element>\n" +
            "    </xs:sequence>\n" +
            "  </xs:complexType>\n" +
            "</xs:schema>")
            .ignoreWhitespace()
            .areSimilar();
    }

    public static class WithXmlElements {
        public WithXmlElements() {

        }
        @XmlElements({
            @XmlElement(type = Integer.class, name = "integer"),
            @XmlElement(type = String.class, name = "string")
        }
        )
        @XmlDocumentation(value = "some docu about this integer", name="integer")
        @XmlDocumentation(value = "some docu about this object")
        List<Object> elements;
    }

    @Test
    public void xmlElements() throws JAXBException, IOException, TransformerException {
        DocumentationAdder adder = new DocumentationAdder(WithXmlElements.class);
        adder.setDebug(true);
        String string = adder.write();
        assertThat(string).and("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" version=\"1.0\">\n" +
                "  <xs:complexType name=\"withXmlElements\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                    "  <!--documentation key: {}withXmlElements (not found)-->\n" +
                    "  <xs:sequence>\n" +
                    "    <xs:choice maxOccurs=\"unbounded\" minOccurs=\"0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <xs:element name=\"integer\" type=\"xs:int\">\n" +
                "    <!--documentation key: {}withXmlElements|ELEMENT|integer-->\n" +
                "    <xs:annotation>\n" +
                "      <xs:documentation>some docu about this integer</xs:documentation>\n" +
                "    </xs:annotation>\n" +
                "  </xs:element>\n" +
                "  <xs:element name=\"string\" type=\"xs:string\">\n" +
                "    <!--documentation key: {}withXmlElements|ELEMENT|string-->\n" +
                "    <xs:annotation>\n" +
                "      <xs:documentation>some docu about this object</xs:documentation>\n" +
                "    </xs:annotation>\n" +
                "  </xs:element>\n" +
                "</xs:choice>\n" +
                    "  </xs:sequence>\n" +
                    "</xs:complexType>" +
                "</xs:schema>")
            .ignoreWhitespace()
            .areSimilar();
    }

}
