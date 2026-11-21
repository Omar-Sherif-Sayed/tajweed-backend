package io.github.omarsherifsayed.tajweed.general.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Decides the language of each request from its {@code Accept-Language} header.
 *
 * <p>Only the 22 languages of {@link SupportedLanguages} are accepted; a region is ignored ({@code zh-CN} is answered
 * in {@code zh}); anything else falls back to Arabic.
 */
@Configuration(proxyBeanMethods = false)
public class LocaleConfig {

    /**
     * Created by Spring.
     */
    public LocaleConfig() {
        // no state
    }

    /**
     * The resolver Spring MVC uses for every request (Spring looks for the bean name {@code localeResolver}).
     *
     * @return an {@code Accept-Language} resolver limited to the product's languages, Arabic by default
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SupportedLanguages.LOCALES);
        resolver.setDefaultLocale(SupportedLanguages.DEFAULT);
        return resolver;
    }
}
