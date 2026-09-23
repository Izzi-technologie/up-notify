package com.wayscompany.webhookalarm.model

enum class Severity(val rank: Int) {
    INFO(1),
    WARNING(2),
    CRITICAL(3),
    ;

    companion object {
        fun fromRaw(value: String?): Severity = when (value?.lowercase()) {
            "warning" -> WARNING
            "critical" -> CRITICAL
            else -> INFO
        }
    }
}
