package mega.privacy.android.app.fragments.homepage.model

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Sort By Header State
 *
 * @property viewType
 */
data class SortByHeaderState(
    val viewType: ViewType = ViewType.LIST,
)