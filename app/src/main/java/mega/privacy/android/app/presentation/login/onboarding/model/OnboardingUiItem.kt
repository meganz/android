package mega.privacy.android.app.presentation.login.onboarding.model

import androidx.compose.runtime.Immutable

/**
 * A model represents the onboarding ui items
 *
 * @property drawableImageResources The list of images for the onboarding
 * @property titles The list of titles
 * @property subtitles The list of subtitles
 */
@Immutable
data class OnboardingUiItem(
    val drawableImageResources: List<Int>,
    val titles: List<String>,
    val subtitles: List<String>,
)
