package mega.privacy.android.domain.usecase.zipbrowser

import mega.privacy.android.domain.repository.ZipArchiveRepository
import javax.inject.Inject

/**
 * Checks a password against an archive before it is used for an extraction, so a wrong password can
 * be reported as such instead of surfacing as a failed entry.
 */
class ValidateZipPasswordUseCase @Inject constructor(
    private val zipArchiveRepository: ZipArchiveRepository,
) {

    suspend operator fun invoke(archivePath: String, password: CharArray): Boolean =
        zipArchiveRepository.isPasswordValid(archivePath, password)
}
