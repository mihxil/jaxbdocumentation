package org.meeuw.xml.bind.annotation;

import java.lang.annotation.*;


/**
 * @author Michiel Meeuwissen
 * @since 0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Repeatable(XmlDocumentations.class)
public @interface XmlDocumentation {
    /**
     *
     * @return The documentation to the xsd element, attribute or type
     */
    String value();


    /**
     * @deprecated
     */
    @Deprecated
    String namespace() default "";

    /**
     * When using multiple {@link javax.xml.bind.annotation.XmlElement}, this can indicate to which one the annotation must refer
     */
    String name() default "";

}
