/**
 * The Quran bundle of the phone applications (the content pipeline's decision D1, {@code docs/plan/backend-plan.md} §8
 * of the documentation repository): a SQLite file with the Quran text, its metadata and the search text, written by
 * the very import migrations that fill the server's database, so that the phones and the server cannot differ.
 *
 * <p>This source set is a build tool, not part of the running application: its SQLite driver never enters the
 * application's classpath. It runs with {@code ./gradlew quranBundle} and with every {@code ./gradlew build}.
 */
package io.github.omarsherifsayed.tajweed.bundle;
