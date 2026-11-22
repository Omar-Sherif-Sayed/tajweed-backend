package io.github.omarsherifsayed.tajweed.business.hadith.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HadeethEnc's API ({@code https://hadeethenc.com/api/v1}, no key). Every answer is returned as received — a string —
 * so that its SHA-256 can be recorded and nothing is changed by a conversion.
 */
@HttpExchange(accept = "application/json")
public interface HadeethEncClient {

    /**
     * The languages HadeethEnc publishes.
     *
     * @return a JSON array of {@code {code, native}}
     */
    @GetExchange("/languages")
    String languages();

    /**
     * Every category, with its title in a language.
     *
     * @param language a language code
     * @return a JSON array of {@code {id, title, hadeeths_count, parent_id}}
     */
    @GetExchange("/categories/list/")
    String categories(@RequestParam("language") String language);

    /**
     * One page of the hadiths of a category (its sub-categories included).
     *
     * @param language   a language code
     * @param categoryId the category
     * @param page       the page, from 1
     * @param perPage    hadiths per page (at most 100)
     * @return a JSON object {@code {data: [{id, title, translations}], meta: {current_page, last_page, total_items}}}
     */
    @GetExchange("/hadeeths/list/")
    String hadiths(@RequestParam("language") String language, @RequestParam("category_id") int categoryId,
                   @RequestParam("page") int page, @RequestParam("per_page") int perPage);

    /**
     * One hadith in one language.
     *
     * @param language a language code
     * @param id       the hadith
     * @return a JSON object with {@code title, hadeeth, attribution, grade, explanation, hints, categories,
     *         translations, hadeeth_intro, words_meanings, reference}
     */
    @GetExchange("/hadeeths/one/")
    String hadith(@RequestParam("language") String language, @RequestParam("id") int id);
}
