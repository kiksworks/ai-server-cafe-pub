package ai_server_cafe.updater;

import javax.annotation.meta.When;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Methods or Classes which annotated this will be ignored in app:test
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludedTest {
    When when() default When.ALWAYS;
}
