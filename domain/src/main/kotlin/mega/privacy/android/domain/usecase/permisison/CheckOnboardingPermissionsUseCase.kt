package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import javax.inject.Inject

/**
 * Use case to check if onboarding permissions are needed for the single activity implementation
 */
class CheckOnboardingPermissionsUseCase @Inject constructor(
    private val isFirstLaunchUseCase: IsFirstLaunchUseCase,
    private val hasNotificationPermissionUseCase: HasNotificationPermissionUseCase,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
) {
    suspend operator fun invoke(): OnboardingPermissionsCheckResult {
        val isFirstTime = isFirstLaunchUseCase()
        val hasNotificationPermission = hasNotificationPermissionUseCase()
        val hasMediaPermissions = hasMediaPermissionUseCase()

        val requestPermissions =
            isFirstTime && (!hasNotificationPermission || !hasMediaPermissions)
        val onlyShowNotificationPermission = hasMediaPermissions && !hasNotificationPermission

        return OnboardingPermissionsCheckResult(
            requestPermissionsOnFirstLaunch = requestPermissions,
            onlyShowNotificationPermission = onlyShowNotificationPermission
        )
    }
}
