package mega.privacy.android.app.main.megaachievements

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.achievement.AchievementType

/**
 * Map AchievementsType to Achievements Info's attributes in InfoAchievementsFragment
 * @see InfoAchievementsFragment
 */
fun AchievementType?.toInfoAchievementsAttribute(isAwarded: Boolean): InfoAchievementsAttribute {
    return when (this) {
        AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL -> {
            InfoAchievementsAttribute(
                R.drawable.ic_install_mobile_big,
                if (isAwarded) R.string.result_paragraph_info_achievement_install_mobile_app else R.string.paragraph_info_achievement_install_mobile_app
            )
        }

        AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL -> {
            InfoAchievementsAttribute(
                R.drawable.ic_install_mega_big,
                if (isAwarded) R.string.result_paragraph_info_achievement_install_desktop else R.string.paragraph_info_achievement_install_desktop
            )
        }

        AchievementType.MEGA_ACHIEVEMENT_WELCOME -> InfoAchievementsAttribute(
            R.drawable.ic_registration_big,
            R.string.result_paragraph_info_achievement_registration
        )

        else -> throw IllegalArgumentException("InfoAchievementsFragment doesn't recognize the achievement type")
    }
}

/**
 * InfoAchievementsAttribute to hold the icon and text in InfoAchievementsFragment
 * @see InfoAchievementsFragment
 * @property iconResourceId
 * @property subtitleTextResourceId
 */
data class InfoAchievementsAttribute(
    @DrawableRes val iconResourceId: Int,
    @StringRes val subtitleTextResourceId: Int,
)