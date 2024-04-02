package mega.privacy.android.app.presentation.search.mapper

import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.domain.entity.search.SearchCategory
import javax.inject.Inject

/**
 * Mapper used to map the type filters to categories in search API
 */
class TypeFilterToSearchMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param typeFilterOption type filter used for search
     */
    operator fun invoke(typeFilterOption: TypeFilterOption?): SearchCategory =
        when (typeFilterOption) {
            TypeFilterOption.Audio -> SearchCategory.AUDIO
            TypeFilterOption.Video -> SearchCategory.VIDEO
            TypeFilterOption.Images -> SearchCategory.IMAGES
            TypeFilterOption.Documents -> SearchCategory.ALL_DOCUMENTS
            else -> SearchCategory.ALL
        }
}
