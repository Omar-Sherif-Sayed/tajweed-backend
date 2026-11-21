package io.github.omarsherifsayed.tajweed.general.config;

import jakarta.servlet.Filter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web settings shared by every endpoint: CORS for the web application and HTTP caching ({@code ETag}).
 *
 * <p>Caching: content changes only with a database migration or an import, so every {@code /v1/**} answer gets an
 * {@code ETag} (a hash of its body). A client that sends it back in {@code If-None-Match} receives {@code 304 Not
 * Modified} without the body — fast for the phones, light for the server. Each controller adds its own
 * {@code Cache-Control} and {@code Vary: Accept-Language}.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** The application's settings (the allowed web origins). */
    private final AppProperties properties;

    /**
     * Allows the web application's origins to call {@code /v1/**} from a browser — reading only (GET), with the
     * language header, and exposing the caching header.
     *
     * @param registry Spring MVC's CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/v1/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "HEAD", "OPTIONS")
                .allowedHeaders(HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.IF_NONE_MATCH, HttpHeaders.CONTENT_TYPE)
                .exposedHeaders(HttpHeaders.ETAG)
                .allowCredentials(false)
                .maxAge(3600);
    }

    /**
     * Adds an {@code ETag} to every {@code /v1/**} answer and answers {@code 304} when the client already has it.
     *
     * @return the filter registration, limited to the public API
     */
    @Bean
    public FilterRegistrationBean<Filter> etagFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.setUrlPatterns(List.of("/v1/*"));
        registration.setName("etagFilter");
        return registration;
    }
}
