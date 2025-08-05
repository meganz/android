package mega.privacy.android.feature.sync.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.feature.sync.ui.SyncHostActivity
import mega.privacy.android.navigation.DeeplinkProcessor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeeplinkProcessor] implementation for Sync feature
 */
@Singleton
class SyncDeeplinkProcessor @Inject constructor() : DeeplinkProcessor {

    override fun matches(deeplink: String): Boolean =
        deeplink.contains("https://$MEGA_NZ_DOMAIN_NAME/${getSyncRoute()}")
                || deeplink.contains("https://$MEGA_APP_DOMAIN_NAME/${getSyncRoute()}")

    override fun execute(context: Context, deeplink: String) {
        val intent = Intent(context, SyncHostActivity::class.java)
        intent.data = Uri.parse(deeplink)
        context.startActivity(intent)
    }
}