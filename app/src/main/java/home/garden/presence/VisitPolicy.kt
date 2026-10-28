package home.garden.presence

/** Pure policy: a due event is not permission to interrupt an active phone. */
object VisitPolicy {
    fun quiet(minute: Int, start: Int, end: Int): Boolean = when {
        start == end -> true
        start < end -> minute in start until end
        else -> minute >= start || minute < end
    }
    fun skipReason(enabled: Boolean, explicitTest: Boolean, interactive: Boolean,
        displayOff: Boolean, aod: Boolean, includeAod: Boolean, permission: Boolean,
        inQuietHours: Boolean, powerSave: Boolean, inCall: Boolean,
        count: Int, cap: Int, sinceLastMs: Long): String? = when {
        !enabled && !explicitTest -> "disabled"
        !permission -> "permission_missing"
        interactive -> "skipped_interactive"
        !displayOff && !(aod && includeAod) -> "skipped_display_or_aod"
        inCall -> "skipped_call"
        powerSave -> "skipped_battery_saver"
        inQuietHours && !explicitTest -> "quiet_hours"
        count >= cap && !explicitTest -> "daily_limit"
        sinceLastMs < 30 * 60_000L && !explicitTest -> "cooldown"
        else -> null
    }
}
