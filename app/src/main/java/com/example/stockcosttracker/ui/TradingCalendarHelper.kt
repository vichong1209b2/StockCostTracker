package com.example.stockcosttracker.ui

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate

class TradingCalendarHelper(
    private val prefs: SharedPreferences
) {

    companion object {
        private const val KEY_CACHED_HOLIDAY_YEAR = "cached_holiday_year"
        private const val KEY_CACHED_HOLIDAY_DATES = "cached_holiday_dates"
        private const val KEY_CACHED_HOLIDAY_FETCH_DATE = "cached_holiday_fetch_date"

        private const val TWSE_EN_HOLIDAY_PAGE_URL =
            "https://www.twse.com.tw/en/trading/holiday.html"

        private const val TWSE_ZH_HOLIDAY_HTML_URL =
            "https://www.twse.com.tw/holidaySchedule/holidaySchedule?response=html"

        private val FALLBACK_MARKET_HOLIDAYS: Set<LocalDate> = setOf(
            // 2025
            LocalDate.parse("2025-01-01"),
            LocalDate.parse("2025-01-27"),
            LocalDate.parse("2025-01-28"),
            LocalDate.parse("2025-01-29"),
            LocalDate.parse("2025-01-30"),
            LocalDate.parse("2025-01-31"),
            LocalDate.parse("2025-02-28"),
            LocalDate.parse("2025-04-03"),
            LocalDate.parse("2025-04-04"),
            LocalDate.parse("2025-05-01"),
            LocalDate.parse("2025-05-30"),
            LocalDate.parse("2025-10-06"),
            LocalDate.parse("2025-10-10"),

            // 2026
            LocalDate.parse("2026-01-01"),
            LocalDate.parse("2026-02-12"),
            LocalDate.parse("2026-02-13"),
            LocalDate.parse("2026-02-15"),
            LocalDate.parse("2026-02-16"),
            LocalDate.parse("2026-02-17"),
            LocalDate.parse("2026-02-18"),
            LocalDate.parse("2026-02-19"),
            LocalDate.parse("2026-02-20"),
            LocalDate.parse("2026-02-27"),
            LocalDate.parse("2026-02-28"),
            LocalDate.parse("2026-04-03"),
            LocalDate.parse("2026-04-04"),
            LocalDate.parse("2026-04-05"),
            LocalDate.parse("2026-04-06"),
            LocalDate.parse("2026-05-01"),
            LocalDate.parse("2026-06-19"),
            LocalDate.parse("2026-09-25"),
            LocalDate.parse("2026-09-28"),
            LocalDate.parse("2026-10-09"),
            LocalDate.parse("2026-10-10"),
            LocalDate.parse("2026-10-25"),
            LocalDate.parse("2026-10-26"),
            LocalDate.parse("2026-12-25"),

            // 2027
            LocalDate.parse("2027-01-01"),
            LocalDate.parse("2027-02-08"),
            LocalDate.parse("2027-02-09"),
            LocalDate.parse("2027-02-10"),
            LocalDate.parse("2027-02-11"),
            LocalDate.parse("2027-02-12"),
            LocalDate.parse("2027-02-15"),
            LocalDate.parse("2027-02-28"),
            LocalDate.parse("2027-04-05"),
            LocalDate.parse("2027-05-01"),
            LocalDate.parse("2027-06-10"),
            LocalDate.parse("2027-09-15"),
            LocalDate.parse("2027-10-10")
        )
    }

    @Volatile
    private var remoteHolidayCache: Set<LocalDate> = loadCachedHolidayDates()

    suspend fun ensureTradingCalendarLoaded() {
        val today = LocalDate.now()
        val cachedYear = prefs.getInt(KEY_CACHED_HOLIDAY_YEAR, -1)
        val fetchedDate = prefs.getString(KEY_CACHED_HOLIDAY_FETCH_DATE, null)

        val shouldRefresh = cachedYear != today.year || fetchedDate != today.toString()
        if (!shouldRefresh && remoteHolidayCache.isNotEmpty()) return

        val fetched = fetchTwseHolidayDatesSafely(today.year)
        if (fetched.isNotEmpty()) {
            remoteHolidayCache = fetched
            saveCachedHolidayDates(today.year, fetched, today)
        }
    }

    fun isTaiwanTradingDay(date: LocalDate): Boolean {
        val isWeekend = when (date.dayOfWeek) {
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> true
            else -> false
        }
        if (isWeekend) return false

        val holidaySet = getEffectiveHolidaySetFor(date.year)
        return date !in holidaySet
    }

    fun getEffectiveHolidaySetFor(year: Int): Set<LocalDate> {
        val remote = remoteHolidayCache.filter { it.year == year }.toSet()
        return if (remote.isNotEmpty()) {
            remote
        } else {
            FALLBACK_MARKET_HOLIDAYS.filter { it.year == year }.toSet()
        }
    }

    private fun loadCachedHolidayDates(): Set<LocalDate> {
        val raw = prefs.getString(KEY_CACHED_HOLIDAY_DATES, null).orEmpty()
        if (raw.isBlank()) return emptySet()

        return raw.split(",")
            .mapNotNull { token ->
                runCatching { LocalDate.parse(token.trim()) }.getOrNull()
            }
            .toSet()
    }

    private fun saveCachedHolidayDates(
        year: Int,
        dates: Set<LocalDate>,
        fetchedDate: LocalDate
    ) {
        val raw = dates.sorted().joinToString(",") { it.toString() }
        prefs.edit()
            .putInt(KEY_CACHED_HOLIDAY_YEAR, year)
            .putString(KEY_CACHED_HOLIDAY_DATES, raw)
            .putString(KEY_CACHED_HOLIDAY_FETCH_DATE, fetchedDate.toString())
            .apply()
    }

    private suspend fun fetchTwseHolidayDatesSafely(year: Int): Set<LocalDate> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val enPageHtml = downloadUrl(TWSE_EN_HOLIDAY_PAGE_URL)
                val csvUrl = extractTwseHolidayCsvUrl(enPageHtml)

                val csvDates = if (!csvUrl.isNullOrBlank()) {
                    val csvText = downloadUrl(csvUrl)
                    parseTwseHolidayCsv(csvText, year)
                } else {
                    emptySet()
                }

                if (csvDates.isNotEmpty()) {
                    csvDates
                } else {
                    val zhHtml = downloadUrl(TWSE_ZH_HOLIDAY_HTML_URL)
                    parseTwseHolidayHtmlTable(zhHtml, year)
                }
            }.getOrElse {
                emptySet()
            }
        }
    }

    private fun downloadUrl(urlString: String): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Android) StockCostTracker/1.0"
        )

        return try {
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractTwseHolidayCsvUrl(html: String): String? {
        val normalized = html.replace(Regex("\\s+"), " ")
        val regex = Regex("""href="([^"]+?\.csv[^"]*)"""", RegexOption.IGNORE_CASE)
        val match = regex.find(normalized) ?: return null
        val raw = match.groupValues[1]

        return when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.startsWith("/") -> "https://www.twse.com.tw$raw"
            else -> "https://www.twse.com.tw/$raw"
        }
    }

    private fun parseTwseHolidayCsv(csvText: String, year: Int): Set<LocalDate> {
        val result = linkedSetOf<LocalDate>()
        val lines = csvText.lines()
        val dateRegex = Regex("""\b$year-\d{2}-\d{2}\b""")

        lines.forEach { line ->
            val dateText = dateRegex.find(line)?.value ?: return@forEach
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@forEach
            val lower = line.lowercase()

            if (isTradingDayMarker(lower)) return@forEach

            result += date
        }

        return result
    }

    private fun parseTwseHolidayHtmlTable(html: String, year: Int): Set<LocalDate> {
        val result = linkedSetOf<LocalDate>()
        val normalized = html.replace(Regex("\\s+"), " ")

        val rowRegex = Regex("""<tr[^>]*>(.*?)</tr>""", setOf(RegexOption.IGNORE_CASE))
        val dateRegex = Regex("""\b$year-\d{2}-\d{2}\b""")

        rowRegex.findAll(normalized).forEach { rowMatch ->
            val row = rowMatch.groupValues[1]
            val dateText = dateRegex.find(row)?.value ?: return@forEach
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: return@forEach

            val plainRow = stripHtmlTags(row).lowercase()
            if (isTradingDayMarker(plainRow)) return@forEach

            result += date
        }

        return result
    }

    private fun stripHtmlTags(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isTradingDayMarker(text: String): Boolean {
        return text.contains("開始交易") ||
                text.contains("最後交易") ||
                text.contains("begins trading") ||
                text.contains("starts trading") ||
                text.contains("first trading day") ||
                text.contains("last trading day")
    }
}