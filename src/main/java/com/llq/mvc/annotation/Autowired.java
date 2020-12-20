package com.llq.mvc.annotation;

import java.lang.annotation.*;

/**
 * @author Buffer
 * @date 2020/8/5 8:21
 * @Description
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}