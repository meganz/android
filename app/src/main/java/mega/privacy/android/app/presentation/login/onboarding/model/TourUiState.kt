package mega.privacy.android.app.presentation.login.onboarding.model

import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.login.onboarding.view.TourScreen

/**
 * UI state class for [TourScreen].
 *
 * @property meetingLink The meeting link to be joined.
 * @property errorTextId The error text string resource ID.
 * @property shouldOpenLink Whether we need to open the meeting link or not.
 */
data class TourUiState(
    val meetingLink: String = "",
    @StringRes val errorTextId: Int? = null,
    val shouldOpenLink: Boolean = false,
)
