package org.meeuw.jaxbdocumentation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Michiel Meeuwissen
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XmlDocumentation {
	String value();
	String qname();
}
