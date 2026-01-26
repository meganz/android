package mega.privacy.android.app.fragments.homepage.banner

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.listeners.SessionTransferURLListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.utils.Constants.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase.Companion.SESSION_STRING
import mega.privacy.mobile.analytics.event.PwmSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.TransferItSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.VpnSmartBannerItemSelectedEvent

/**
 * Take actions when the user clicking on a banner
 * @param fragment The Homepage fragment which contains the banners
 */
class BannerClickHandler(private val fragment: HomepageFragment) :
    BannerAdapter.ClickBannerCallback {

    companion object {
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
            "megaad\\.app",
        )

        /**
         * Internal VPN URL matching implementation
         */
        private fun matchesVpnUrl(link: String): Boolean {
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

        /**
         * Internal PWM URL matching implementation
         */
        private fun matchesPwmUrl(link: String): Boolean {
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
         * Internal Transfer-it URL matching implementation
         */
        private fun matchesTransferItUrl(link: String): Boolean {
            val transferItPatterns = MEGA_DOMAIN_PATTERNS.map { domain ->
                "^https://$domain/transfer-it.*"
            }
            return transferItPatterns.any { pattern -> link.matches(pattern.toRegex()) }
        }

        /**
         * Helper method to generate VPN URL
         */
        private fun megaVpnUrl(domainName: String) = "https://vpn.$domainName"

        /**
         * Helper method to generate PWM URL
         */
        private fun megaPwmUrl(domainName: String) = "https://pwm.$domainName"
    }

    override fun actOnActionLink(link: String) {
        val context = fragment.requireContext()

        when {
            matchesAchievementUrl(link) -> {
                val intent = Intent(context, AchievementsFeatureActivity::class.java)
                context.startActivity(intent)
            }

            matchesReferralUrl(link) -> {
                checkIfLinkRequiresTransferSession(context, link)
            }

            matchesSettingsUrl(link) -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }

            matchesTextEditorUrl(link) -> {
                (fragment.activity as ManagerActivity).showNewTextFileDialog(null)
            }

            matchesVpnUrl(link) -> {
                Analytics.tracker.trackEvent(VpnSmartBannerItemSelectedEvent)
                openInSpecificApp(context, link, MEGA_VPN_PACKAGE_NAME)
            }

            matchesPwmUrl(link) -> {
                Analytics.tracker.trackEvent(PwmSmartBannerItemSelectedEvent)
                openInSpecificApp(context, link, MEGA_PASS_PACKAGE_NAME)
            }

            matchesTransferItUrl(link) -> {
                Analytics.tracker.trackEvent(TransferItSmartBannerItemSelectedEvent)
                context.launchUrl(link)
            }

            else -> {
                context.launchUrl(link)
            }
        }
    }

    private fun checkIfLinkRequiresTransferSession(context: Context, url: String) {
        if (url.contains(SESSION_STRING)) {
            val start = url.indexOf(SESSION_STRING)
            if (start != -1) {
                val path = url.substring(start + SESSION_STRING.length)
                if (!isTextEmpty(path)) {
                    MegaApplication.getInstance().megaApi.getSessionTransferURL(
                        path,
                        SessionTransferURLListener(context)
                    )
                }
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

    private fun matchesAchievementUrl(link: String) =
        link == "https://$MEGA_NZ_DOMAIN_NAME/achievements"
                || link == "https://$MEGA_APP_DOMAIN_NAME/achievements"

    private fun matchesReferralUrl(link: String) =
        link == "https://$MEGA_NZ_DOMAIN_NAME/fm/refer"
                || link == "https://$MEGA_APP_DOMAIN_NAME/fm/refer"

    private fun matchesSettingsUrl(link: String) =
        link == "https://$MEGA_NZ_DOMAIN_NAME/appSettings"
                || link == "https://$MEGA_APP_DOMAIN_NAME/appSettings"

    private fun matchesTextEditorUrl(link: String) =
        link == "https://$MEGA_NZ_DOMAIN_NAME/newText"
                || link == "https://$MEGA_APP_DOMAIN_NAME/newText"
}
