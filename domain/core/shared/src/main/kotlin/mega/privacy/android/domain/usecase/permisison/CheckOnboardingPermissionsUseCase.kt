package mega.privacy.android.domain.usecase.permisison

import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
import javax.inject.Inject

/**
 * Use case to check if onboarding permissions are needed for the single activity implementation
 */
class CheckOnboardingPermissionsUseCase @Inject constructor(
    private val hasNotificationPermissionUseCase: HasNotificationPermissionUseCase,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
) {
    operator fun invoke(): OnboardingPermissionsCheckResult {
        val hasNotificationPermission = hasNotificationPermissionUseCase()
        val hasMediaPermissions = hasMediaPermissionUseCase()

        val requestPermissions = !hasNotificationPermission || !hasMediaPermissions
        val onlyShowNotificationPermission = hasMediaPermissions && !hasNotificationPermission

        return OnboardingPermissionsCheckResult(
            requestPermissionsOnFirstLaunch = requestPermissions,
            onlyShowNotificationPermission = onlyShowNotificationPermission
        )
    }
}
