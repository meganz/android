package mega.privacy.android.feature.sync.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.feature.sync.ui.SyncHostActivity
import mega.privacy.android.navigation.DeeplinkProcessor
import mega.privacy.android.shared.sync.domain.IsSyncFeatureEnabledUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeeplinkProcessor] implementation for Sync feature
 * @property isSyncFeatureEnabledUseCase Use case to check if sync feature is enabled
 */
@Singleton
class SyncDeeplinkProcessor @Inject constructor(
    private val isSyncFeatureEnabledUseCase: IsSyncFeatureEnabledUseCase,
) : DeeplinkProcessor {

    override fun matches(deeplink: String): Boolean =
        if (isSyncFeatureEnabledUseCase()) {
            deeplink.contains("https://mega.nz/${getSyncRoute()}")
        } else {
            false
        }

    override fun execute(context: Context, deeplink: String) {
        val intent = Intent(context, SyncHostActivity::class.java)
        intent.data = Uri.parse(deeplink)
        context.startActivity(intent)
    }
}