package com.domain.app.ui.data

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import com.domain.app.core.plugin.ExportFormat

/**
 * Time frame options for data export
 */
enum class TimeFrame(val displayName: String) {
    DAY("Last 24 Hours"),
    WEEK("Last Week"),
    MONTH("Last Month"),
    THREE_MONTHS("Last 3 Months"),
    SIX_MONTHS("Last 6 Months"),
    YEAR("Last Year"),
    ALL("All Time"),
    CUSTOM("Custom Range");
    
    /**
     * Calculate date range based on time frame
     */
    fun toDateRange(): Pair<Instant, Instant> {
        val now = Instant.now()
        val start = when (this) {
            DAY -> now.minus(1, ChronoUnit.DAYS)
            WEEK -> now.minus(7, ChronoUnit.DAYS)
            MONTH -> now.minus(30, ChronoUnit.DAYS)
            THREE_MONTHS -> now.minus(90, ChronoUnit.DAYS)
            SIX_MONTHS -> now.minus(180, ChronoUnit.DAYS)
            YEAR -> now.minus(365, ChronoUnit.DAYS)
            ALL -> Instant.EPOCH
            CUSTOM -> now.minus(7, ChronoUnit.DAYS) // Default to last week
        }
        return start to now
    }
}

/**
 * Export options for data export dialog
 */
data class ExportOptions(
    val format: ExportFormat,
    val timeFrame: TimeFrame,
    val selectedPlugins: Set<String>,
    val customDateRange: Pair<Instant, Instant>? = null,
    val encrypt: Boolean = false
) {
    /**
     * Get the actual date range to use for export
     */
    fun getDateRange(): Pair<Instant, Instant> {
        return if (timeFrame == TimeFrame.CUSTOM && customDateRange != null) {
            customDateRange
        } else {
            timeFrame.toDateRange()
        }
    }
}
