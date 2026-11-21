package io.github.omarsherifsayed.tajweed.general.config;

import io.github.omarsherifsayed.tajweed.general.i18n.SupportedLanguages;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 * The OpenAPI document: the contract that the web, Android and iPhone clients read.
 *
 * <p>springdoc builds it from the controllers; it is served at {@code /v3/api-docs} (JSON) and
 * {@code /v3/api-docs.yaml}, and shown at {@code /swagger-ui.html}. An integration test writes it to
 * {@code docs/api/openapi.yaml}, and CI fails when the committed file is out of date.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    /**
     * Created by Spring.
     */
    public OpenApiConfig() {
        // no state
    }

    /**
     * The document's title, description, version and licence.
     *
     * @param properties the application's settings (the API version)
     * @return the OpenAPI description of this API
     */
    @Bean
    public OpenAPI tajweedOpenApi(AppProperties properties) {
        return new OpenAPI().info(new Info()
                .title("Tajweed - The Noble Quran and Its Sciences — public API")
                .description("Public, read-only API of «تجويد - القرآن الكريم وعلومه». No login, no accounts. "
                        + "Every answer is a BaseResponse; texts are in the language of the Accept-Language header "
                        + "(22 languages, Arabic by default).")
                .version(properties.app().apiVersion())
                .license(new License().name("MIT").url("https://opensource.org/license/mit")));
    }

    /**
     * Documents the {@code Accept-Language} header on every operation, with the 22 accepted values.
     *
     * @return a customiser springdoc applies to each operation
     */
    @Bean
    public OperationCustomizer acceptLanguageHeader() {
        return (operation, handlerMethod) -> operation.addParametersItem(new HeaderParameter()
                .name(HttpHeaders.ACCEPT_LANGUAGE)
                .required(false)
                .description("The language of the texts in the answer; Arabic when absent or not supported.")
                .schema(new StringSchema()._enum(SupportedLanguages.CODES)._default("ar")));
    }
}
