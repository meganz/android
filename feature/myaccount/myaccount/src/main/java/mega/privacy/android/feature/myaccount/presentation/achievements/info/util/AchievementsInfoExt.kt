package mega.privacy.android.feature.myaccount.presentation.achievements.info.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.achievement.AchievementType
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Map AchievementsType to Achievements Info's attributes in AchievementsInfoFragment
 */
fun AchievementType?.toAchievementsInfoAttribute(
    isAwarded: Boolean,
    durationInDays: Int,
): AchievementInfoAttribute {
    return when (this) {
        AchievementType.MEGA_ACHIEVEMENT_MOBILE_INSTALL -> {
            AchievementInfoAttribute(
                this.name,
                iconPackR.drawable.ic_install_mobile_big,
                if (isAwarded) {
                    sharedR.string.result_paragraph_info_achievement_install_mobile_app
                } else {
                    if (durationInDays == 0) {
                        sharedR.string.paragraph_info_achievement_install_mobile_app_permanent_duration
                    } else {
                        sharedR.string.paragraph_info_achievement_install_mobile_app_dynamic_duration
                    }
                }
            )
        }

        AchievementType.MEGA_ACHIEVEMENT_DESKTOP_INSTALL -> {
            AchievementInfoAttribute(
                this.name,
                iconPackR.drawable.ic_install_mega_big,
                if (isAwarded) {
                    sharedR.string.result_paragraph_info_achievement_install_desktop
                } else {
                    if (durationInDays == 0) {
                        sharedR.string.paragraph_info_achievement_install_desktop_permanent_duration
                    } else {
                        sharedR.string.paragraph_info_achievement_install_desktop_dynamic_duration
                    }
                }
            )
        }

        AchievementType.MEGA_ACHIEVEMENT_WELCOME -> AchievementInfoAttribute(
            this.name,
            iconPackR.drawable.ic_registration_big,
            sharedR.string.result_paragraph_info_achievement_registration
        )

        else -> throw IllegalArgumentException("AchievementsInfoFragment doesn't recognize the achievement type")
    }
}

/**
 * AchievementInfoAttribute to hold the icon and text in AchievementsInfoFragment
 * @property name
 * @property iconResourceId
 * @property subtitleTextResourceId
 */
data class AchievementInfoAttribute(
    val name: String,
    @DrawableRes val iconResourceId: Int,
    @StringRes val subtitleTextResourceId: Int,
)
