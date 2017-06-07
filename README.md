jaxb-documentation
==================

Makes it possible to create also xsd:documentation tags with JAXB

```java


	public static final String NS = "http://meeuw.org/a";

	@XmlDocumentation(value = "some docu about a", qname="{" + NS + "}a")
	@XmlType(namespace = NS)
	public static class A {

		@XmlElement(namespace = NS)
		B b;
	}

	@XmlDocumentation(value = "docu about b", qname = "{" + NS + "}b")
	@XmlType(namespace = NS)
	public static class B {

	}


	@Test
	public void collect() throws JAXBException, IOException, SAXException, TransformerException, ParserConfigurationException {
		Collector collector = new Collector(A.class);
		StringWriter writer = new StringWriter();
		for (Map.Entry<String, Source> sourceEntry : schemaSources(A.class).entrySet()) {
			collector.transform(sourceEntry.getValue(), new StreamResult(writer));
		}
		assertThat(writer.toString()).isXmlEqualTo("<?xml version=\"1.0\" encoding=\"utf-8\"?><xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:tns=\"http://meeuw.org/a\" targetNamespace=\"http://meeuw.org/a\" version=\"1.0\">\n" +
            "<xs:complexType><xs:documentation>some docu about a</xs:documentation><xs:sequence><xs:element form=\"qualified\" minOccurs=\"0\" name=\"b\" type=\"tns:b\"/></xs:sequence></xs:complexType>\n" +
            "<xs:complexType><xs:documentation>docu about b</xs:documentation><xs:sequence/></xs:complexType>\n" +
            "</xs:schema>");

	}
```

