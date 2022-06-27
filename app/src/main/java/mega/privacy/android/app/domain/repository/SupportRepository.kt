package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Support repository
 *
 */
interface SupportRepository {
    /**
     * Log ticket
     *
     * @param ticketContent
     */
    suspend fun logTicket(ticketContent: String)

    /**
     * Upload file
     *
     * @param file
     * @return progress
     */
    fun uploadFile(file: File): Flow<Float>


    /**
     * Get support email
     *
     * @return support email address
     */
    suspend fun getSupportEmail(): String
}
