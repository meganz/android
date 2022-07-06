package mega.privacy.android.domain.usecase

/**
 * Create trace string from trace elements list
 */
fun interface CreateTraceString {
    /**
     * Invoke
     *
     * @param trace
     * @param loggingClasses
     * @return trace string or null
     */
    suspend operator fun invoke(
        trace: List<StackTraceElement>,
        loggingClasses: List<String>,
    ): String?
}