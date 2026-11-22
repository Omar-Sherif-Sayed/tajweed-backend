package io.github.omarsherifsayed.tajweed.business.hadith.service.impl;

import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncCategory;
import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncCategoryTitle;
import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadithCategory;
import io.github.omarsherifsayed.tajweed.business.hadith.entity.HadeethEncHadithText;
import io.github.omarsherifsayed.tajweed.business.hadith.mapper.HadithMapper;
import io.github.omarsherifsayed.tajweed.business.hadith.repository.HadeethEncCategoryRepository;
import io.github.omarsherifsayed.tajweed.business.hadith.repository.HadeethEncCategoryTitleRepository;
import io.github.omarsherifsayed.tajweed.business.hadith.repository.HadeethEncHadithCategoryRepository;
import io.github.omarsherifsayed.tajweed.business.hadith.repository.HadeethEncHadithRepository;
import io.github.omarsherifsayed.tajweed.business.hadith.repository.HadeethEncHadithTextRepository;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadith;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithCategories;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithCategory;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithPage;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithSummary;
import io.github.omarsherifsayed.tajweed.business.hadith.schema.response.DtoHadithText;
import io.github.omarsherifsayed.tajweed.business.hadith.service.HadithRetrievalService;
import io.github.omarsherifsayed.tajweed.general.exception.BaseException;
import io.github.omarsherifsayed.tajweed.general.exception.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads HadeethEnc's hadiths as the sync stored them. A title or a text is given in the request's language when
 * HadeethEnc publishes it, otherwise in Arabic — and the answer says which.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HadithRetrievalServiceImpl implements HadithRetrievalService {

    /** Arabic, the language of every hadith. */
    private static final String ARABIC = "ar";

    /** Reads the categories. */
    private final HadeethEncCategoryRepository categoryRepository;

    /** Reads their titles. */
    private final HadeethEncCategoryTitleRepository categoryTitleRepository;

    /** Reads the hadiths. */
    private final HadeethEncHadithRepository hadithRepository;

    /** Reads the hadiths' categories. */
    private final HadeethEncHadithCategoryRepository hadithCategoryRepository;

    /** Reads the texts. */
    private final HadeethEncHadithTextRepository hadithTextRepository;

    /**
     * {@inheritDoc}
     *
     * @param locale the request's language
     * @return the categories
     */
    @Override
    public DtoHadithCategories getCategories(Locale locale) {
        String language = locale.getLanguage();
        List<HadeethEncCategory> categories = categoryRepository.findAll();
        Map<Integer, Set<Integer>> subtree = subtrees(categories);
        Map<Integer, Set<Integer>> hadithsByCategory = hadithsByCategory();
        Map<String, String> titles = titles(language);
        List<DtoHadithCategory> list = new ArrayList<>();
        for (HadeethEncCategory c : categories.stream().sorted((a, b) -> a.getId() - b.getId()).toList()) {
            Set<Integer> hadiths = new HashSet<>();
            for (int sub : subtree.get(c.getId())) {
                hadiths.addAll(hadithsByCategory.getOrDefault(sub, Set.of()));
            }
            String own = titles.get(c.getId() + "|" + language);
            String title = own != null ? own : titles.get(c.getId() + "|" + ARABIC);
            list.add(new DtoHadithCategory(c.getId(), c.getParentId(), title, own != null ? language : ARABIC,
                    hadiths.size()));
        }
        return new DtoHadithCategories(list, HadithMapper.HADEETHENC);
    }

    /**
     * {@inheritDoc}
     *
     * @param categoryId the category
     * @param page       the page, from 0
     * @param size       the page's size
     * @param locale     the request's language
     * @return the page
     */
    @Override
    public DtoHadithPage getHadiths(int categoryId, int page, int size, Locale locale) {
        List<HadeethEncCategory> categories = categoryRepository.findAll();
        Map<Integer, Set<Integer>> subtree = subtrees(categories);
        if (!subtree.containsKey(categoryId)) {
            throw new BaseException(ErrorCode.NOT_FOUND_HADITH_CATEGORY);
        }
        Map<Integer, Set<Integer>> hadithsByCategory = hadithsByCategory();
        TreeSet<Integer> ids = new TreeSet<>();
        for (int sub : subtree.get(categoryId)) {
            ids.addAll(hadithsByCategory.getOrDefault(sub, Set.of()));
        }
        List<Integer> pageIds = ids.stream().skip((long) page * size).limit(size).toList();
        String language = locale.getLanguage();
        Map<Integer, HadeethEncHadithText> own = new HashMap<>();
        Map<Integer, HadeethEncHadithText> arabic = new HashMap<>();
        for (HadeethEncHadithText t : hadithTextRepository.findByHadithIdInAndLanguageIn(pageIds, languages(language))) {
            (t.getLanguage().equals(language) ? own : arabic).put(t.getHadithId(), t);
        }
        List<DtoHadithSummary> list = new ArrayList<>();
        for (Integer id : pageIds) {
            HadeethEncHadithText t = own.containsKey(id) ? own.get(id) : arabic.get(id);
            if (t != null) {
                list.add(new DtoHadithSummary(id, t.getTitle(), t.getLanguage()));
            }
        }
        return new DtoHadithPage(categoryId, list, page, size, ids.size(), HadithMapper.HADEETHENC);
    }

    /**
     * {@inheritDoc}
     *
     * @param hadithId the hadith
     * @param locale   the request's language
     * @return the hadith
     */
    @Override
    public DtoHadith getHadith(int hadithId, Locale locale) {
        if (!hadithRepository.existsById(hadithId)) {
            throw new BaseException(ErrorCode.NOT_FOUND_HADITH);
        }
        String language = locale.getLanguage();
        DtoHadithText arabic = null;
        DtoHadithText translation = null;
        for (HadeethEncHadithText t : hadithTextRepository.findByHadithIdAndLanguageIn(hadithId, languages(language))) {
            if (t.getLanguage().equals(ARABIC)) {
                arabic = HadithMapper.toDto(t);
            } else {
                translation = HadithMapper.toDto(t);
            }
        }
        if (arabic == null) {
            throw new BaseException(ErrorCode.NOT_FOUND_HADITH);
        }
        List<Integer> categoryIds = hadithCategoryRepository.findByHadithIdOrderByCategoryIdAsc(hadithId).stream()
                .map(HadeethEncHadithCategory::getCategoryId).toList();
        String pageLanguage = translation != null ? language : ARABIC;
        return new DtoHadith(hadithId, arabic, translation, categoryIds, HadithMapper.sourceUrl(hadithId, pageLanguage),
                HadithMapper.HADEETHENC);
    }

    /**
     * Every category with the ids of itself and all its sub-categories.
     *
     * @param categories every category
     * @return category id → its subtree's ids
     */
    private static Map<Integer, Set<Integer>> subtrees(List<HadeethEncCategory> categories) {
        Map<Integer, List<Integer>> children = new HashMap<>();
        for (HadeethEncCategory c : categories) {
            if (c.getParentId() != null) {
                children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        Map<Integer, Set<Integer>> subtree = new HashMap<>();
        for (HadeethEncCategory c : categories) {
            Set<Integer> all = new HashSet<>();
            List<Integer> stack = new ArrayList<>(List.of(c.getId()));
            while (!stack.isEmpty()) {
                Integer next = stack.removeLast();
                if (all.add(next)) {
                    stack.addAll(children.getOrDefault(next, List.of()));
                }
            }
            subtree.put(c.getId(), all);
        }
        return subtree;
    }

    /**
     * Every category's own hadiths.
     *
     * @return category id → hadith ids
     */
    private Map<Integer, Set<Integer>> hadithsByCategory() {
        Map<Integer, Set<Integer>> map = new HashMap<>();
        for (HadeethEncHadithCategory link : hadithCategoryRepository.findAll()) {
            map.computeIfAbsent(link.getCategoryId(), k -> new HashSet<>()).add(link.getHadithId());
        }
        return map;
    }

    /**
     * The categories' titles in a language and in Arabic.
     *
     * @param language the request's language
     * @return {@code "category|language"} → title
     */
    private Map<String, String> titles(String language) {
        Map<String, String> map = new HashMap<>();
        for (HadeethEncCategoryTitle t : categoryTitleRepository.findByLanguageIn(languages(language))) {
            map.put(t.getCategoryId() + "|" + t.getLanguage(), t.getTitle());
        }
        return map;
    }

    /**
     * The request's language and Arabic, once each (the request may itself be in Arabic).
     *
     * @param language the request's language
     * @return the languages to read
     */
    private static Set<String> languages(String language) {
        return new LinkedHashSet<>(List.of(language, ARABIC));
    }
}
