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
 * Path segment this processor matches on. Was the classic navigation route for the feature.
 *
 * Kept as a literal because the graph that defined it is gone, not because the value is known to
 * be reachable: nothing injects [mega.privacy.android.navigation.DeeplinkHandler] since
 * AND-23319 gutted OpenLinkActivity, so no sync deeplink resolves today. Whether
 * `mega.nz/Sync` is meant to work is a product question — see SAT-2365.
 */
internal const val SYNC_DEEPLINK_PATH = "Sync"


/**
 * [DeeplinkProcessor] implementation for Sync feature
 */
@Singleton
class SyncDeeplinkProcessor @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : DeeplinkProcessor {

    override fun matches(deeplink: String): Boolean =
        deeplink.contains("https://$MEGA_NZ_DOMAIN_NAME/$SYNC_DEEPLINK_PATH")
                || deeplink.contains("https://$MEGA_APP_DOMAIN_NAME/$SYNC_DEEPLINK_PATH")

    override fun execute(context: Context, deeplink: String) {
        if (deeplink.contains("$SYNC_DEEPLINK_PATH/SyncNewFolder")) {
            megaNavigator.openNewSync(context, syncType = SyncType.TYPE_TWOWAY)
        } else {
            megaNavigator.openSyncs(context)
        }
    }
}
