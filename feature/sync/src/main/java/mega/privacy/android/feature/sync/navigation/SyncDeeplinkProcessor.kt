package mega.privacy.android.feature.sync.navigation

import android.content.Context
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.navigation.DeeplinkProcessor
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject
import javax.inject.Singleton


/**
 * [DeeplinkProcessor] implementation for Sync feature
 */
@Singleton
class SyncDeeplinkProcessor @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : DeeplinkProcessor {

    override fun matches(deeplink: String): Boolean =
        deeplink.contains("https://$MEGA_NZ_DOMAIN_NAME/${getSyncRoute()}")
                || deeplink.contains("https://$MEGA_APP_DOMAIN_NAME/${getSyncRoute()}")

    override fun execute(context: Context, deeplink: String) {
        if (deeplink.contains("${getSyncRoute()}/SyncNewFolder")) {
            megaNavigator.openNewSync(context, syncType = SyncType.TYPE_TWOWAY)
        } else {
            megaNavigator.openSyncs(context)
        }
    }
}
