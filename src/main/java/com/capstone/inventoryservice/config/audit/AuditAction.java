package com.capstone.inventoryservice.config.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String action();
    String module();
    String severity() default "Low";
    boolean sensitive() default false;
    String targetType() default "Event";
}
