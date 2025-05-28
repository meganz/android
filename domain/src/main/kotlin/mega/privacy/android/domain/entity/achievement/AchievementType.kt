package mega.privacy.android.domain.entity.achievement

import kotlinx.serialization.Serializable

/**
 * Achievement Type identifier
 *
 * @property classValue: Value defined in megaapi.h SDK class
 */
@Serializable
enum class AchievementType(val classValue: Int) {

    /**
     * Achievement for welcoming user after creating account
     */
    MEGA_ACHIEVEMENT_WELCOME(1),

    /**
     * Achievement when user invites friends
     */
    MEGA_ACHIEVEMENT_INVITE(3),

    /**
     * Achievement when user installs MEGASync
     */
    MEGA_ACHIEVEMENT_DESKTOP_INSTALL(4),

    /**
     * Achievement when user install Mega mobile app
     */
    MEGA_ACHIEVEMENT_MOBILE_INSTALL(5),

    /**
     * Achievement when user select mega pwm free trial
     */
    MEGA_ACHIEVEMENT_MEGA_PWM_TRIAL(10),

    /**
     * Achievement when user select mega vpn free trial
     */
    MEGA_ACHIEVEMENT_MEGA_VPN_TRIAL(11),

    /**
     * Invalid Achievement
     */
    INVALID_ACHIEVEMENT(-1)
}