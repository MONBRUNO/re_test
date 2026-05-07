package com.example.Naengbuhae.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
    String action();      // 예: "DELETE"
    String targetType();  // 예: "RECIPE"
    String description() default ""; // 커스텀 설명
}