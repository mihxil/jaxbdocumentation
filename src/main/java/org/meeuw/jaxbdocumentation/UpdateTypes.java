package org.meeuw.jaxbdocumentation;

import lombok.Getter;

import java.util.function.Supplier;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamSource;

/**
 * @author Michiel Meeuwissen
 * @since 0.4
 */
public class UpdateTypes implements Supplier<Transformer> {

    @Getter
    private final Class<?>[] classes;
    private Transformer transformer;


    public UpdateTypes(Class<?>...classes) {
        this.classes = classes;
    }

    @Override
    public Transformer get() {
        if (transformer == null) {
            try {
                TransformerFactory transFact = TransformerFactory.newInstance();
                transformer = transFact.newTransformer(
                    new StreamSource(UpdateTypes.class.getResourceAsStream("/update-types.xslt")));
                //transformer.setURIResolver(new DocumentationAdder.DocumentationResolver(createDocumentations(classes)));

            } catch (TransformerConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
        return transformer;

    }
}
