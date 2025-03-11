package mega.privacy.android.app.presentation.search.mapper

import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.TypeFilterOption
import javax.inject.Inject

/**
 * Mapper used to map the type filters to categories in search API
 */
class TypeFilterToSearchMapper @Inject constructor() {
    /**
     * invoke
     *
     * @param typeFilterOption type filter used for search
     * @param nodeSourceType the source type of the search, used when typeFilterOption is not provided
     */
    operator fun invoke(
        typeFilterOption: TypeFilterOption?,
        nodeSourceType: NodeSourceType,
    ): SearchCategory = typeFilterOption?.let {
        when (it) {
            TypeFilterOption.Audio -> SearchCategory.AUDIO
            TypeFilterOption.Video -> SearchCategory.VIDEO
            TypeFilterOption.Images -> SearchCategory.IMAGES
            TypeFilterOption.Documents -> SearchCategory.DOCUMENTS
            TypeFilterOption.Pdf -> SearchCategory.PDF
            TypeFilterOption.Presentation -> SearchCategory.PRESENTATION
            TypeFilterOption.Spreadsheet -> SearchCategory.SPREADSHEET
            TypeFilterOption.Folder -> SearchCategory.FOLDER
            TypeFilterOption.Other -> SearchCategory.OTHER
        }
    } ?: when (nodeSourceType) {
        NodeSourceType.FAVOURITES -> SearchCategory.FAVOURITES
        NodeSourceType.DOCUMENTS -> SearchCategory.ALL_DOCUMENTS
        NodeSourceType.AUDIO -> SearchCategory.AUDIO
        else -> SearchCategory.ALL
    }
}
