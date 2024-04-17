package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.search.TypeFilterOption

/**
 * Data class for type filter and string res
 *
 * @param type type filter
 * @param title string res for type filter
 */
data class TypeFilterWithName(val type: TypeFilterOption, @StringRes val title: Int)
