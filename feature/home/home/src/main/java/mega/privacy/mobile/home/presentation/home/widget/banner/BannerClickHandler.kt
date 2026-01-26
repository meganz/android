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
import mega.privacy.mobile.analytics.event.TransferItSmartBannerItemSelectedEvent
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

            matchesTransferItUrl(url) -> {
                Analytics.tracker.trackEvent(TransferItSmartBannerItemSelectedEvent)
                navigationHandler.navigate(WebSiteNavKey(url))
            }

            else -> {
                // Fallback for other MEGA URLs
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
            } catch (_: Exception) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageName".toUri()
                    )
                )
            }
        }
    }

    /**
     * MEGA domain patterns for URL matching
     */
    private val MEGA_DOMAIN_PATTERNS = arrayOf(
        "mega\\.co\\.nz",
        "mega\\.nz",
        "mega\\.io",
        "mega\\.app",
        "megaad\\.co\\.nz",
        "megaad\\.nz",
        "megaad\\.io",
        "megaad\\.app"
    )

    fun matchesVpnUrl(link: String): Boolean {
        // Check existing specific domains
        if (link.startsWith(megaVpnUrl(MEGA_NZ_DOMAIN_NAME)) ||
            link.startsWith(megaVpnUrl(MEGA_APP_DOMAIN_NAME))
        ) {
            return true
        }

        // Check all MEGA domain variations for subdomain format (vpn.domain.*)
        val vpnSubdomainPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
            "^https://vpn\\.$domain.*"
        }

        // Check all MEGA domain variations for path format (domain.*/vpn)
        val vpnPathPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
            "^https://$domain/vpn.*"
        }

        return vpnSubdomainPatterns.any { pattern -> link.matches(pattern.toRegex()) } ||
                vpnPathPatterns.any { pattern -> link.matches(pattern.toRegex()) }
    }

    fun matchesPwmUrl(link: String): Boolean {
        // Check existing specific domains
        if (link.startsWith(megaPwmUrl(MEGA_NZ_DOMAIN_NAME)) ||
            link.startsWith(megaPwmUrl(MEGA_APP_DOMAIN_NAME))
        ) {
            return true
        }

        // Check all MEGA domain variations for subdomain format (pwm.domain.*)
        val pwmSubdomainPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
            "^https://pwm\\.$domain.*"
        }

        // Check all MEGA domain variations for path format (domain.*/pass)
        val pwmPathPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
            "^https://$domain/pass.*"
        }

        return pwmSubdomainPatterns.any { pattern -> link.matches(pattern.toRegex()) } ||
                pwmPathPatterns.any { pattern -> link.matches(pattern.toRegex()) }
    }

    /**
     * Check if URL matches Transfer-it patterns
     */
    fun matchesTransferItUrl(link: String): Boolean {
        val transferItPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
            "^https://$domain/transfer-it.*"
        }
        return transferItPatterns.any { pattern -> link.matches(pattern.toRegex()) }
    }

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
