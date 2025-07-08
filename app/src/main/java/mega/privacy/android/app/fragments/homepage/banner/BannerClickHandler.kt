package mega.privacy.android.app.fragments.homepage.banner

import android.content.Context
import android.content.Intent
import android.net.Uri
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.utils.Constants.MEGA_PASS_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_PASS_URL
import mega.privacy.android.app.utils.Constants.MEGA_VPN_PACKAGE_NAME
import mega.privacy.android.app.utils.Constants.MEGA_VPN_URL
import mega.privacy.android.app.utils.LinksUtil
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
            link == ACHIEVEMENT -> {
                val intent = Intent(context, AchievementsFeatureActivity::class.java)
                context.startActivity(intent)
            }

            link == REFERRAL -> {
                LinksUtil.requiresTransferSession(context, link)
            }

            link == SETTINGS -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }

            link == TEXT_EDITOR -> {
                (fragment.activity as ManagerActivity).showNewTextFileDialog(null)
            }

            link.startsWith(MEGA_VPN_URL) -> {
                Analytics.tracker.trackEvent(VpnSmartBannerItemSelectedEvent)
                openInSpecificApp(context, link, MEGA_VPN_PACKAGE_NAME)
            }

            link.startsWith(MEGA_PASS_URL) -> {
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
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                setPackage(packageName)
                context.startActivity(this)
            }
        }.onFailure {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$packageName")
                    )
                )
            } catch (exception: Exception) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    )
                )
            }
        }
    }

    companion object {
        private const val ACHIEVEMENT = "https://mega.nz/achievements"
        private const val REFERRAL = "https://mega.nz/fm/refer"
        private const val SETTINGS = "https://mega.nz/appSettings"
        private const val TEXT_EDITOR = "https://mega.nz/newText"
    }
}