package mega.privacy.android.domain.entity.permission

/**
 * Result of permissions check
 */
data class OnboardingPermissionsCheckResult(
    val requestPermissionsOnFirstLaunch: Boolean,
    val onlyShowNotificationPermission: Boolean,
)
