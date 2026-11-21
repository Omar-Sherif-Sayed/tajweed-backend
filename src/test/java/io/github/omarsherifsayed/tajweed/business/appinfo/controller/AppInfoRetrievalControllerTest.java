package io.github.omarsherifsayed.tajweed.business.appinfo.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.omarsherifsayed.tajweed.business.appinfo.service.impl.AppInfoRetrievalServiceImpl;
import io.github.omarsherifsayed.tajweed.general.config.AppProperties;
import io.github.omarsherifsayed.tajweed.general.i18n.LocaleConfig;
import io.github.omarsherifsayed.tajweed.general.i18n.MessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Proves the web behaviour of {@code GET /v1/app-info} and of the shared error handling: the answer's envelope, its
 * language (Arabic by default, the 21 others on request, Arabic for an unsupported one), the caching and security
 * headers, and the error answers for an unknown path and a wrong method.
 *
 * <p>A web slice (no database): the controller, the real service and the shared web configuration.
 */
@WebMvcTest(AppInfoRetrievalController.class)
@Import({AppInfoRetrievalServiceImpl.class, MessageService.class, LocaleConfig.class})
@EnableConfigurationProperties(AppProperties.class)
class AppInfoRetrievalControllerTest {

    /** Sends requests to the web layer without a server. */
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Without Accept-Language the answer is in Arabic, inside BaseResponse")
    void answersInArabicByDefault() throws Exception {
        // given / when / then
        mockMvc.perform(get("/v1/app-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").value("تم بنجاح"))
                .andExpect(jsonPath("$.data.name").value("تجويد - القرآن الكريم وعلومه"))
                .andExpect(jsonPath("$.data.shortName").value("تجويد"))
                .andExpect(jsonPath("$.data.author.name").value("عمر شريف"))
                .andExpect(jsonPath("$.data.author.link").value("https://www.linkedin.com/in/omarsherif21/"))
                .andExpect(jsonPath("$.data.apiVersion").value("1"))
                .andExpect(jsonPath("$.data.language").value("ar"))
                .andExpect(jsonPath("$.data.languages.length()").value(22));
    }

    @Test
    @DisplayName("Accept-Language: en gives the English texts")
    void answersInEnglish() throws Exception {
        mockMvc.perform(get("/v1/app-info").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.name").value("Tajweed - The Noble Quran and Its Sciences"))
                .andExpect(jsonPath("$.data.author.name").value("Omar Sherif"))
                .andExpect(jsonPath("$.data.language").value("en"));
    }

    @Test
    @DisplayName("A region is ignored: zh-CN is answered in zh")
    void ignoresTheRegion() throws Exception {
        mockMvc.perform(get("/v1/app-info").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data.language").value("zh"));
    }

    @Test
    @DisplayName("An unsupported language falls back to Arabic")
    void fallsBackToArabic() throws Exception {
        mockMvc.perform(get("/v1/app-info").header(HttpHeaders.ACCEPT_LANGUAGE, "nl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("تم بنجاح"))
                .andExpect(jsonPath("$.data.language").value("ar"));
    }

    @Test
    @DisplayName("The answer may be cached for an hour, varies with the language, and carries the security headers")
    void sendsCachingAndSecurityHeaders() throws Exception {
        mockMvc.perform(get("/v1/app-info"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600, public"))
                .andExpect(header().stringValues(HttpHeaders.VARY, hasItem(HttpHeaders.ACCEPT_LANGUAGE)))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("An unknown path answers 404 NOT_FOUND_PATH in the requested language")
    void answersNotFoundForAnUnknownPath() throws Exception {
        mockMvc.perform(get("/v1/no-such-path").header(HttpHeaders.ACCEPT_LANGUAGE, "en"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND_PATH"))
                .andExpect(jsonPath("$.message").value("This path does not exist."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("A wrong method answers 405 METHOD_NOT_ALLOWED in Arabic by default")
    void answersMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/v1/app-info"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("هذه الطريقة غير مسموحة لهذا المسار."));
    }
}
