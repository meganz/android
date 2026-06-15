package mega.privacy.mobile.home.presentation.home.widget.domore.model

import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem

/**
 * UI state for the "Do more with MEGA" home widget.
 *
 * [mega.privacy.android.domain.featuretoggle.ApiFeatures.DoMoreWithMEGA] feature flag).
 * @property items the action items to display, already sorted by their order.
 */
data class DoMoreWithMegaUiState(
    val items: List<DoMoreWithMegaItem> = emptyList(),
)
