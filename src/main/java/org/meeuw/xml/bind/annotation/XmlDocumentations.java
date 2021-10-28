package org.meeuw.xml.bind.annotation;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface XmlDocumentations {

    XmlDocumentation[] value();
}
