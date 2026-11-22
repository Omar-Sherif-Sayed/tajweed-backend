/**
 * The tafsir module (F5, F26): the tafsirs chosen with the author ({@code content/reviewed/tafsirs.csv}), each ayah's
 * text from each of them.
 *
 * <p>The texts come from the Quran Foundation's Content Sync — their terms: «Content Sync is the only permitted path for
 * obtaining and maintaining an offline copy», synced at least every 7 days, and their content never in a public
 * repository ({@code AGENTS.md} §7.1). The credentials live only in the environment ({@code QF_CLIENT_ID},
 * {@code QF_CLIENT_SECRET}, {@code QF_ENV}); without them this module stays empty and the rest of the backend works.
 * Every text is stored and served exactly as received (HTML).
 */
package io.github.omarsherifsayed.tajweed.business.tafsir;
