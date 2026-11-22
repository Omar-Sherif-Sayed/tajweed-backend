package io.github.omarsherifsayed.tajweed.business.hadith.client;

import io.github.omarsherifsayed.tajweed.business.hadith.scheduler.HadeethEncProperties;
import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Builds the {@link HadeethEncClient} on {@code RestClient}, with the address and time-out of
 * {@code tajweed.hadeethenc.*}.
 */
@Configuration
public class HadeethEncClientConfig {

    /**
     * Created by Spring.
     */
    public HadeethEncClientConfig() {
        // no state
    }

    /**
     * The client.
     *
     * @param properties the address and the time-out
     * @return the HTTP interface, ready to call
     */
    @Bean
    public HadeethEncClient hadeethEncClient(HadeethEncProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(properties.timeout())
                .build());
        factory.setReadTimeout(properties.timeout());
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("User-Agent", "tajweed-backend (github.com/Omar-Sherif-Sayed/tajweed-backend)")
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(HadeethEncClient.class);
    }
}
