package com.domain.app.social.contracts

data class ShareableDataPoint(
    val id: String,
    val title: String,
    val preview: String,
    val privacyRisk: PrivacyRisk
)

enum class PrivacyRisk {
    LOW, MEDIUM, HIGH, CRITICAL
}
