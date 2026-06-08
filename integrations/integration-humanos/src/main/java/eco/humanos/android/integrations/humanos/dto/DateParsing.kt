package eco.humanos.android.integrations.humanos.dto

import java.time.Instant

/**
 * HumanOS (Prisma + Next.js `NextResponse.json`) serializes `DateTime` fields as
 * ISO-8601 strings (e.g. `"2026-06-08T00:00:00.000Z"`), NOT epoch millis. The
 * app's domain models use epoch millis, so DTO date fields are carried as
 * [String] and converted here.
 *
 * Parsing is defensive: any unexpected/blank value yields `null` rather than
 * throwing, so one odd date never fails deserialization of a whole payload.
 * `Instant.parse` is available from API 26 (our minSdk).
 */
internal fun isoToMillis(iso: String?): Long? =
    iso?.takeIf { it.isNotBlank() }
        ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
