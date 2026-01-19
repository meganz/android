package mega.privacy.mobile.home.presentation.home.widget.banner

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.mobile.analytics.event.PwmSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.VpnSmartBannerItemSelectedEvent

/**
 * Handles banner click actions based on URL patterns
 */
object BannerClickHandler {

    /**
     * Handle banner click action
     *
     * @param context Context for launching activities
     * @param navigationHandler Navigation handler for navigation actions
     * @param url The URL from the banner
     */
    fun handleBannerClick(
        context: Context,
        navigationHandler: NavigationHandler,
        url: String,
    ) {
        when {
            matchesVpnUrl(url) -> {
                Analytics.tracker.trackEvent(VpnSmartBannerItemSelectedEvent)
                openInSpecificApp(context, url, MEGA_VPN_PACKAGE_NAME)
            }

            matchesPwmUrl(url) -> {
                Analytics.tracker.trackEvent(PwmSmartBannerItemSelectedEvent)
                openInSpecificApp(context, url, MEGA_PASS_PACKAGE_NAME)
            }

            else -> {
                navigationHandler.navigate(WebSiteNavKey(url))
            }
        }
    }

    private fun openInSpecificApp(context: Context, link: String, packageName: String) {
        runCatching {
            Intent(Intent.ACTION_VIEW, link.toUri()).apply {
                setPackage(packageName)
                context.startActivity(this)
            }
        }.onFailure {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "market://details?id=$packageName".toUri()
                    )
                )
            } catch (exception: Exception) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
                    )
                )
            }
        }
    }

    private fun matchesVpnUrl(link: String) =
        link.startsWith(megaVpnUrl(MEGA_NZ_DOMAIN_NAME))
                || link.startsWith(megaVpnUrl(MEGA_APP_DOMAIN_NAME))

    private fun matchesPwmUrl(link: String) =
        link.startsWith(megaPwmUrl(MEGA_NZ_DOMAIN_NAME))
                || link.startsWith(megaPwmUrl(MEGA_APP_DOMAIN_NAME))

    /**
     * Url for VPN
     */
    private fun megaVpnUrl(domainName: String) = "https://vpn.$domainName"

    /**
     * Url for MEGA PWM
     */
    private fun megaPwmUrl(domainName: String) = "https://pwm.$domainName"
    private const val MEGA_VPN_PACKAGE_NAME: String = "mega.vpn.android.app"
    private const val MEGA_PASS_PACKAGE_NAME: String = "mega.pwm.android.app"
}
