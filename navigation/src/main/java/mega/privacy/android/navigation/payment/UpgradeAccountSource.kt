package mega.privacy.android.navigation.payment

import mega.privacy.android.domain.entity.payment.UpgradeSource

/**
 * Enum class to define the source of the upgrade account.
 */
enum class UpgradeAccountSource {
    /**
     * Navigate from unknown source.
     */
    UNKNOWN,

    /**
     * Navigate from ads free screen.
     */
    ADS_FREE_SCREEN,

    /**
     * Navigate from my account screen.
     */
    MY_ACCOUNT_SCREEN,

    /**
     * Navigate from settings screen.
     */
    SETTINGS_SCREEN
}

fun UpgradeAccountSource.toSource() = when (this) {
    UpgradeAccountSource.MY_ACCOUNT_SCREEN -> UpgradeSource.MyAccount
    UpgradeAccountSource.SETTINGS_SCREEN -> UpgradeSource.Settings
    else -> UpgradeSource.Main
}