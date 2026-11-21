/**
 * The public, read-only REST API of «تجويد - القرآن الكريم وعلومه» (Tajweed - The Noble Quran and Its Sciences).
 *
 * <p>Two parts: {@code general} holds what every module shares (the response envelope, error codes, languages,
 * CORS, rate limiting, HTTP caching, OpenAPI); {@code business} holds one package per product module (app-info first,
 * then quran, reciter, tafsir …), each in the same layers: controller, service, repository, entity, schema, mapper.
 * The rules behind every choice are in the documentation repository: {@code AGENTS.md} §5, §6, §9 and
 * {@code docs/plan/backend-plan.md}.
 */
package io.github.omarsherifsayed.tajweed;
