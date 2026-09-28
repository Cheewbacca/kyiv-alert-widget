package ua.kyiv.alertwidget

/** Maps only documented threats. An unknown active cause must never look like an all-clear. */
enum class AlertStatus { CLEAR, YELLOW, RED, ACTIVE_UNKNOWN }

object AlertStatusMapper {
    private val red = setOf("missile", "ballistic", "missile-drone", "massive-drone")
    private val yellow = setOf("drone")

    fun from(state: Int, causes: List<String>): AlertStatus {
        return when (state) {
            0 -> {
                require(causes.isEmpty()) { "Clear state cannot have active causes" }
                AlertStatus.CLEAR
            }
            1 -> when {
                causes.any { it in red } -> AlertStatus.RED
                causes.isNotEmpty() && causes.all { it in yellow } -> AlertStatus.YELLOW
                else -> AlertStatus.ACTIVE_UNKNOWN
            }
            else -> throw IllegalArgumentException("Unknown alert state: $state")
        }
    }
}
