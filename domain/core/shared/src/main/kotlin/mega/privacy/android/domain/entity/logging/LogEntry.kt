package mega.privacy.android.domain.entity.logging

/**
 * File log message
 *
 * @property tag
 * @property message
 * @property stackTrace
 * @property priority
 * @property throwable
 */
data class LogEntry(
    val tag: String? = null,
    val message: String,
    val stackTrace: String? = null,
    val priority: Int,
    val throwable: Throwable? = null,
) {
    override fun toString() = "${tag.orEmpty()} $message ${stackTrace.orEmpty()}".trim()
}