/*
 * File: PortfolioAutoSyncHelper.kt
 * Version: v1.0.0
 * Updated: 2026-05-17
 * Package: com.example.stockcosttracker.ui
 *
 * Purpose:
 * - Encapsulate auto-sync decision logic
 * - Keep PortfolioViewModel focused on orchestration only
 */

package com.example.stockcosttracker.ui

import com.example.stockcosttracker.domain.model.PortfolioSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PortfolioAutoSyncHelper(
    private val tradingCalendarHelper: TradingCalendarHelper
) {

    data class AutoSyncDecision(
        val shouldSync: Boolean,
        val stockCodes: List<String> = emptyList()
    )

    fun evaluateLaunchAutoSync(
        now: LocalDateTime,
        summaries: List<PortfolioSummary>,
        lastAutoSyncDate: LocalDate?
    ): AutoSyncDecision {
        val today = now.toLocalDate()

        val holdingSummaries = summaries.filter { it.holdingShares > 0.0 }

        if (holdingSummaries.isEmpty()) {
            return AutoSyncDecision(shouldSync = false)
        }

        if (!tradingCalendarHelper.isTaiwanTradingDay(today)) {
            return AutoSyncDecision(shouldSync = false)
        }

        if (!isAfterHours(now.toLocalTime())) {
            return AutoSyncDecision(shouldSync = false)
        }

        if (lastAutoSyncDate == today) {
            return AutoSyncDecision(shouldSync = false)
        }

        if (!shouldAutoSyncOnLaunch(holdingSummaries, today)) {
            return AutoSyncDecision(shouldSync = false)
        }

        val stockCodes = holdingSummaries
            .map { it.stockCode }
            .distinct()

        if (stockCodes.isEmpty()) {
            return AutoSyncDecision(shouldSync = false)
        }

        return AutoSyncDecision(
            shouldSync = true,
            stockCodes = stockCodes
        )
    }

    private fun shouldAutoSyncOnLaunch(
        holdingSummaries: List<PortfolioSummary>,
        today: LocalDate
    ): Boolean {
        return holdingSummaries.any { summary ->
            extractQuoteDate(summary.lastQuoteTime) != today
        }
    }

    private fun extractQuoteDate(lastQuoteTime: String?): LocalDate? {
        if (lastQuoteTime.isNullOrBlank()) return null

        return try {
            if (lastQuoteTime.length >= 10) {
                LocalDate.parse(lastQuoteTime.substring(0, 10))
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isAfterHours(now: LocalTime): Boolean {
        return !now.isBefore(AppSyncConfig.AUTO_SYNC_AFTER_HOURS_TIME)
    }
}