package com.christophecvb.touchportal.annotations.processor;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;

public class TPAnnotationException extends Exception {
    private TPAnnotationException(Class<? extends Annotation> annotationType, Boolean isMissing, Integer count, Element element, String typeFor) {
        super(annotationType.getSimpleName() + " Annotation"
                + (isMissing != null && isMissing ? " is missing" : "")
                + (count != null ? " count cannot be " + count : "")
                + (typeFor != null ? " " + typeFor : "")
                + (element != null ? " for element " + element.getSimpleName() : "")
        );
    }

    public static class Builder {
        private final Class<? extends Annotation> annotationType;
        private Boolean isMissing;
        private Integer count;
        private Element element;
        private String typeFor;

        public Builder(Class<? extends Annotation> annotationType) {
            this.annotationType = annotationType;
        }

        public Builder isMissing(Boolean isMissing) {
            this.isMissing = isMissing;
            return this;
        }

        public Builder forElement(Element element) {
            this.element = element;
            return this;
        }

        public Builder typeFor(String type, String forElement, String because) {
            this.typeFor = "type for " + forElement + " cannot be " + type + " because " + because;
            return this;
        }

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public TPAnnotationException build() {
            return new TPAnnotationException(this.annotationType, this.isMissing, this.count, this.element, this.typeFor);
        }
    }
}
