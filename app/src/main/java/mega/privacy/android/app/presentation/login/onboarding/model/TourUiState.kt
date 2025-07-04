package mega.privacy.android.app.presentation.login.onboarding.model

import mega.privacy.android.domain.entity.ThemeMode

/**
 * UI state class for [mega.privacy.android.app.presentation.login.onboarding.view.NewTourScreen].
 *
 * @property themeMode The current theme mode.
 */
data class TourUiState(
    val themeMode: ThemeMode = ThemeMode.System,
)
