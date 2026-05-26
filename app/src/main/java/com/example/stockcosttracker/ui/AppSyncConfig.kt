/*
 * File: AppSyncConfig.kt
 * Version: v1.0.0
 * Updated: 2026-05-17
 * Package: com.example.stockcosttracker.ui
 *
 * Purpose:
 * - Centralize app sync related constants
 * - Keep PortfolioViewModel lightweight
 */

package com.example.stockcosttracker.ui

import java.time.LocalTime

object AppSyncConfig {

    /**
     * After this time, app launch may trigger one-time auto quote sync
     * on a Taiwan trading day if holdings exist and today's quotes are stale.
     */
    val AUTO_SYNC_AFTER_HOURS_TIME: LocalTime = LocalTime.of(13, 50)
}