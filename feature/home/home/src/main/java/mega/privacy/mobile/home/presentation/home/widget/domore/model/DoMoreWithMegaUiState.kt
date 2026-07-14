package mega.privacy.mobile.home.presentation.home.widget.domore.model

import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem

/**
 * UI state for the "Do more with MEGA" home widget.
 *
 * [mega.privacy.android.domain.featuretoggle.ApiFeatures.DoMoreWithMEGA] feature flag).
 * @property items the action items to display, already sorted by their order.
 * @property isCameraUploadsEnabled whether Camera uploads is currently enabled, used to decide
 * whether the Camera uploads shortcut opens its settings or the permissions screen.
 * @property hasPreviouslyEnabledCameraUploads whether the user has enabled Camera uploads before
 * (the enabled preference has been set at least once). When true the Camera uploads shortcut skips
 * the permissions onboarding even if Camera uploads is currently disabled.
 */
data class DoMoreWithMegaUiState(
    val items: List<DoMoreWithMegaItem> = emptyList(),
    val isCameraUploadsEnabled: Boolean = false,
    val hasPreviouslyEnabledCameraUploads: Boolean = false,
)
