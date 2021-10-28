package org.meeuw.jaxbdocumentation;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import javax.validation.constraints.Pattern;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
class UpdateTypesTest {

    public static final String NS = "http://meeuw.org/a";

    @XmlType(namespace = NS)
    public static class A {

        @XmlAttribute
        @Pattern(regexp = "[a-z]{3,}")
        String attr;
    }


    @Test
    public void updateTypes() throws JAXBException, IOException, TransformerException {
        UpdateTypes collector = new UpdateTypes(A.class);
        //collector.setXmlStyleSheet("xs3p.xsl");
        StringWriter writer = new StringWriter();

        for (Map.Entry<String, Source> sourceEntry : Utils.schemaSources(collector.getClasses()).entrySet()) {
            collector.get().transform(sourceEntry.getValue(), new StreamResult(writer));
        }
        XmlAssert.assertThat(writer.toString()).and("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xs:schema targetNamespace=\"http://meeuw.org/a\" version=\"1.0\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <xs:complexType name=\"a\">\n" +
            "        <xs:sequence/>\n" +
            "        <xs:attribute name=\"attr\" type=\"xs:string\"/>\n" +
            "    </xs:complexType>\n" +
            "</xs:schema>")
            .ignoreWhitespace()
            .areSimilar();

    }




}
