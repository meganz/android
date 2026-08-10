package mega.privacy.android.feature.clouddrive.presentation.search.model

import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption

/**
 * Callback result after selecting a filter option
 */
sealed interface SearchFilterResult {
    data class Type(val option: TypeFilterOption?) : SearchFilterResult
    data class DateModified(val option: DateFilterOption?) : SearchFilterResult
    data class DateAdded(val option: DateFilterOption?) : SearchFilterResult
}