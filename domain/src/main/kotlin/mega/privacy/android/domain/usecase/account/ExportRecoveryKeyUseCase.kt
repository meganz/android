package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.file.SaveTextOnContentUriUseCase
import javax.inject.Inject

/**
 * Export recovery use case
 */
class ExportRecoveryKeyUseCase @Inject constructor(
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase,
    private val setMasterKeyExportedUseCase: SetMasterKeyExportedUseCase,
    private val saveTextOnContentUriUseCase: SaveTextOnContentUriUseCase,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(uri: String): Boolean {
        val key = getExportMasterKeyUseCase()
        return if (!key.isNullOrEmpty()) {
            setMasterKeyExportedUseCase()
            saveTextOnContentUriUseCase(uri, key)
        } else {
            false
        }
    }
}