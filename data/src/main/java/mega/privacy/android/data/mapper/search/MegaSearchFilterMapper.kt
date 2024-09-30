package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.NodeType
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchTarget
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
import nz.mega.sdk.MegaSearchFilter
import javax.inject.Inject

/**
 * Mapper create MegaSearchFilter
 * @param searchCategoryIntMapper [SearchCategoryIntMapper]
 * @param dateFilterOptionLongMapper [DateFilterOptionLongMapper]
 */
class MegaSearchFilterMapper @Inject constructor(
    private val searchCategoryIntMapper: SearchCategoryIntMapper,
    private val dateFilterOptionLongMapper: DateFilterOptionLongMapper,
    private val searchTargetIntMapper: SearchTargetIntMapper,
    private val megaNodeTypeMapper: MegaNodeTypeMapper,
    private val sensitivityFilterOptionIntMapper: SensitivityFilterOptionIntMapper,
) {

    /**
     * invoke
     * @param searchQuery [String]
     * @param parentHandle [NodeId]
     * @param searchCategory [SearchCategory]
     * @param modificationDate [DateFilterOption]
     * @param creationDate [DateFilterOption]
     * @param description [String]
     * @param tag [String]
     * @param useAndForTextQuery [Boolean]
     */
    operator fun invoke(
        parentHandle: NodeId? = null,
        searchQuery: String = "",
        searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
        searchCategory: SearchCategory? = SearchCategory.ALL,
        modificationDate: DateFilterOption? = null,
        creationDate: DateFilterOption? = null,
        description: String? = null,
        tag: String? = null,
        useAndForTextQuery: Boolean = true,
        sensitivityFilter: SensitivityFilterOption? = null,
    ): MegaSearchFilter = MegaSearchFilter.createInstance().apply {

        // Set the search query
        if (searchQuery.isNotEmpty()) {
            byName(searchQuery)
        }

        // Set the logical operator to search name, description, tags (AND, OR)
        useAndForTextQuery(useAndForTextQuery)

        // Set the parent node to search, if parentHandle is null, search in all nodes
        if (parentHandle == null || parentHandle.longValue == -1L) {
            byLocation(searchTargetIntMapper(searchTarget))
        } else {
            byLocationHandle(parentHandle.longValue)
        }

        // Set the search category
        searchCategory?.let {
            if (it == SearchCategory.FOLDER) {
                byNodeType(megaNodeTypeMapper(NodeType.FOLDER))
                byCategory(searchCategoryIntMapper(SearchCategory.ALL))
            } else {
                byCategory(searchCategoryIntMapper(it))
            }
        }

        // Set the modification and creation date
        modificationDate?.let {
            dateFilterOptionLongMapper(modificationDate).apply {
                byModificationTime(first, second)
            }
        }

        // Set the creation date
        creationDate?.let {
            dateFilterOptionLongMapper(creationDate).apply {
                byCreationTime(first, second)
            }
        }

        description?.let {
            byDescription(description)
        }

        tag?.let {
            byTag(tag)
        }

        sensitivityFilter?.let {
            bySensitivity(sensitivityFilterOptionIntMapper(sensitivityFilter))
        }
    }
}