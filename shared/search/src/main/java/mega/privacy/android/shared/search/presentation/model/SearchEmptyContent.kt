package mega.privacy.android.shared.search.presentation.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import mega.android.core.ui.model.LocalizedText

/**
 * Content configuration for the landing and empty states of the search shell.
 *
 * @property title Title text.
 * @property description Description text.
 * @property image Drawable resource for the illustration.
 */
@Immutable
data class SearchEmptyContent(
    val title: LocalizedText,
    val description: LocalizedText,
    @DrawableRes val image: Int,
)
