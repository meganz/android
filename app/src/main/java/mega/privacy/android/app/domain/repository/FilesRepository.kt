package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.domain.entity.FolderVersionInfo

interface FilesRepository {
    suspend fun getFolderVersionInfo(): FolderVersionInfo
}
