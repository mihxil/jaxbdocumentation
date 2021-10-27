package org.meeuw.xml.bind.annotation;

import java.lang.annotation.*;


/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})

public @interface XmlDocumentation {
    /**
     *
     * @return The documentation to the xsd element, attribute or type
     */
    String value();

}
