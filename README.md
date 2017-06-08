jaxb-documentation
==================

Makes it possible to create also xsd:documentation tags with JAXB.

This is work in an early stage of development. About this is working now.

```java

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

```

