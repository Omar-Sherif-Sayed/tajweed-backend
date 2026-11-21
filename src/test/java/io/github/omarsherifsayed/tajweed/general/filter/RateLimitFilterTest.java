package io.github.omarsherifsayed.tajweed.general.filter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.omarsherifsayed.tajweed.general.config.AppProperties;
import io.github.omarsherifsayed.tajweed.general.i18n.LocaleConfig;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Proves the rate limiter: a client gets {@code requests-per-minute} requests, then {@code 429 RATE_LIMITED} with
 * {@code Retry-After} and a message in its language; other clients are not affected; paths outside {@code /v1/} are
 * never limited.
 */
class RateLimitFilterTest {

    /** The limit used by these tests: two requests a minute. */
    private static final int LIMIT = 2;

    /** The filter under test. */
    private RateLimitFilter filter;

    /**
     * Builds the filter with the real message files and a limit of two requests a minute.
     */
    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        AppProperties properties = new AppProperties(
                new AppProperties.App("1", "https://www.linkedin.com/in/omarsherif21/"),
                new AppProperties.Cors(List.of("http://localhost:4200")),
                new AppProperties.RateLimit(LIMIT));
        filter = new RateLimitFilter(properties, new LocaleConfig().localeResolver(), new MessageService(source),
                JsonMapper.builder().build());
    }

    @Test
    @DisplayName("The third request in a minute from one address answers 429 in the client's language")
    void limitsAClient() throws Exception {
        // given: two requests are allowed
        assertThat(send("10.0.0.1", "/v1/app-info", "en").getStatus()).isEqualTo(200);
        assertThat(send("10.0.0.1", "/v1/app-info", "en").getStatus()).isEqualTo(200);

        // when: the third one comes
        MockHttpServletResponse third = send("10.0.0.1", "/v1/app-info", "en");

        // then
        assertThat(third.getStatus()).isEqualTo(429);
        assertThat(third.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
        assertThat(third.getContentAsString()).contains("\"code\":\"RATE_LIMITED\"").contains("Too many requests");
    }

    @Test
    @DisplayName("Another address has its own allowance")
    void keepsClientsApart() throws Exception {
        send("10.0.0.1", "/v1/app-info", "ar");
        send("10.0.0.1", "/v1/app-info", "ar");
        assertThat(send("10.0.0.2", "/v1/app-info", "ar").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Paths outside /v1/ (health, OpenAPI) are never limited")
    void doesNotLimitOtherPaths() throws Exception {
        for (int i = 0; i < LIMIT + 3; i++) {
            assertThat(send("10.0.0.3", "/actuator/health", "ar").getStatus()).isEqualTo(200);
        }
    }

    /**
     * Sends one request through the filter.
     *
     * @param address  the client's address
     * @param path     the path
     * @param language the {@code Accept-Language} value
     * @return the response (200 when the request went through to the rest of the chain)
     * @throws Exception if the filter fails
     */
    private MockHttpServletResponse send(String address, String path, String language) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr(address);
        request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, language);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
