package mega.privacy.android.app.presentation.settings.exportrecoverykey

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.LegacyExportRecoveryKeyNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * DeepLinkHandler for export recovery key
 */
class ExportRecoveryKeyDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.EXPORT_MASTER_KEY_LINK -> if (isLoggedIn) {
            listOf(LegacyExportRecoveryKeyNavKey)
        } else {
            snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
            emptyList()
        }

        else -> null
    }

}