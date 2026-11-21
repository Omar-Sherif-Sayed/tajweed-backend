/**
 * Flyway migrations written in Java — used when importing content needs code: reading a source file, verifying its
 * SHA-256 and checking its counts before any row is written. They run in order with the SQL migrations of
 * {@code src/main/resources/db/migration}; Flyway finds both through {@code spring.flyway.locations}.
 */
package db.migration;
