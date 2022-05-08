package mega.privacy.android.app.domain.usecase

import java.io.File

interface GetThumbnail {
    /**
     * get thumbnail from server
     * @return File
     */
    suspend operator  fun invoke(id: Long, base64Id: String): File
}