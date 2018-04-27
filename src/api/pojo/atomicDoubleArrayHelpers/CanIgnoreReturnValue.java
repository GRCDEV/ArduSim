package api.pojo.atomicDoubleArrayHelpers;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that the return value of the annotated method can be safely ignored.
 *
 * <p>This is the opposite of {@link CheckReturnValue}. It can be used inside classes or packages
 * annotated with {@code @CheckReturnValue} to exempt specific methods from the default.
 */
@Documented
@Target({METHOD, TYPE})
@Retention(CLASS)
public @interface CanIgnoreReturnValue {}
