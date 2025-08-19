package mega.privacy.android.app.fragments.homepage.banner

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.utils.Constants.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.app.utils.ConstantsUrl.megaPwmUrl
import mega.privacy.android.app.utils.ConstantsUrl.megaVpnUrl
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_APP_DOMAIN_NAME
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase.Companion.MEGA_NZ_DOMAIN_NAME
import mega.privacy.mobile.analytics.event.PwmSmartBannerItemSelectedEvent
import mega.privacy.mobile.analytics.event.VpnSmartBannerItemSelectedEvent

/**
 * Take actions when the user clicking on a banner
 * @param fragment The Homepage fragment which contains the banners
 */
class BannerClickHandler(private val fragment: HomepageFragment) :
    BannerAdapter.ClickBannerCallback {

    override fun actOnActionLink(link: String) {
        val context = fragment.requireContext()

        when {
            matchesAchievementUrl(link) -> {
                val intent = Intent(context, AchievementsFeatureActivity::class.java)
                context.startActivity(intent)
            }

            matchesReferralUrl(link) -> {
                LinksUtil.requiresTransferSession(context, link)
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

            else -> {
                context.launchUrl(link)
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

    private fun matchesVpnUrl(link: String) =
        link.startsWith(megaVpnUrl(MEGA_NZ_DOMAIN_NAME))
                || link.startsWith(megaVpnUrl(MEGA_APP_DOMAIN_NAME))

    private fun matchesPwmUrl(link: String) =
        link.startsWith(megaPwmUrl(MEGA_NZ_DOMAIN_NAME))
                || link.startsWith(megaPwmUrl(MEGA_APP_DOMAIN_NAME))
}