package org.meeuw.jaxbdocumentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;



/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlDocumentation {
    /**
     * The documentation to the xsd element
     */
    String value();


    String namespace() default "";
    String name() default "";

}
