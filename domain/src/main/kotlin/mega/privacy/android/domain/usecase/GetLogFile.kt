package mega.privacy.android.domain.usecase

import java.io.File

/**
 * Get log file
 */
fun interface GetLogFile {
    /**
     * Invoke
     *
     * @return the log file
     */
    suspend operator fun invoke(): File
}
