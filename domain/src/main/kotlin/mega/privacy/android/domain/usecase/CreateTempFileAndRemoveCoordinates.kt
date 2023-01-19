package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.SyncRecord
import java.io.IOException

/**
 * Create temporary file and remove coordinates
 */
fun interface CreateTempFileAndRemoveCoordinates {
    /**
     * @param root root path
     * @param syncRecord
     */
    @Throws(IOException::class)
    suspend operator fun invoke(
        root: String, syncRecord: SyncRecord,
    ): String?
}
