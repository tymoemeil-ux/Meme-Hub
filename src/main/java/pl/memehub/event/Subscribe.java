package pl.memehub.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Oznacza metode-obserwatora zdarzen w klasie zarejestrowanej w {@link EventBus}.
 * Metoda musi przyjmowac dokladnie jeden argument - typ zdarzenia.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
}
