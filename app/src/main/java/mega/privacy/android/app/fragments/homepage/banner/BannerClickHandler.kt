package mega.privacy.android.app.fragments.homepage.banner

import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.achievements.AchievementsFeatureActivity
import mega.privacy.android.app.presentation.settings.SettingsActivity
import mega.privacy.android.app.utils.LinksUtil

/**
 * Take actions when the user clicking on a banner
 * @param fragment The Homepage fragment which contains the banners
 */
class BannerClickHandler(private val fragment: HomepageFragment) : BannerAdapter.ClickBannerCallback {

    override fun actOnActionLink(link: String) {
        val context = fragment.requireContext()

        when (link) {
            ACHIEVEMENT -> {
                val intent = Intent(context, AchievementsFeatureActivity::class.java)
                context.startActivity(intent)
            }
            REFERRAL -> {
                LinksUtil.requiresTransferSession(context, link)
            }
            SETTINGS -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
            TEXT_EDITOR -> {
                (fragment.activity as ManagerActivity).showNewTextFileDialog(null)
            }
            else -> {
                with(Intent(context, WebViewActivity::class.java)) {
                    data = Uri.parse(link)
                    context.startActivity(this)
                }
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