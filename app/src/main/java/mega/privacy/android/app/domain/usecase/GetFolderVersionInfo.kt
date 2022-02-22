package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.FolderVersionInfo

interface GetFolderVersionInfo {
    suspend operator fun invoke(): FolderVersionInfo?
}