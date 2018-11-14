package greglib.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a member field should be configured using the same Ini object as the containing object.
 * If the member object is another ConfigurableApp, this will probably require the INI to have a separate section.
 *
 * Created by greg on 9/6/17.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChainConfig {
    boolean required() default false;
    String prefix() default "";
}
