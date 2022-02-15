package mega.privacy.android.app.fragments.homepage.banner

import android.content.Intent
import android.net.Uri
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.lollipop.DrawerItem
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity
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
                val intent = Intent(context, AchievementsActivity::class.java)
                context.startActivity(intent)
            }
            REFERRAL -> {
                LinksUtil.requiresTransferSession(context, link)
            }
            SETTINGS -> {
                (fragment.activity as ManagerActivityLollipop).selectDrawerItemLollipop(
                    DrawerItem.SETTINGS)
            }
            TEXT_EDITOR -> {
                (fragment.activity as ManagerActivityLollipop).showNewTextFileDialog(null)
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