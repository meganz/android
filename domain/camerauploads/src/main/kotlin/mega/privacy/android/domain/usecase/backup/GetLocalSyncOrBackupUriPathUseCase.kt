package mega.privacy.android.domain.usecase.backup

import mega.privacy.android.domain.entity.uri.UriPath

interface GetLocalSyncOrBackupUriPathUseCase {

    suspend operator fun invoke(): List<UriPath>
}
