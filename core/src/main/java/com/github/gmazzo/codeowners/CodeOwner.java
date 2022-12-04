package com.github.gmazzo.codeowners;


import java.lang.annotation.*;

@Documented
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CodeOwner {

    String[] value();

}
