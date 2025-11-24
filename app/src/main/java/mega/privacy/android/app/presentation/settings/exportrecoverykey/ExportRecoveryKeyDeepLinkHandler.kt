package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey
import javax.inject.Inject

/**
 * DeepLinkHandler for export recovery key
 */
class ExportRecoveryKeyDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.EXPORT_MASTER_KEY_LINK -> listOf(LegacyExportRecoveryKeyNavKey)

        else -> null
    }

}