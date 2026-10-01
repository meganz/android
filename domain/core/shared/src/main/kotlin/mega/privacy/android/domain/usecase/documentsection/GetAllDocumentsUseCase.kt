package mega.privacy.android.domain.usecase.documentsection

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.DocumentSectionRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

/**
 * The use case for getting all document nodes
 */
class GetAllDocumentsUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val documentSectionRepository: DocumentSectionRepository,
    private val addTypedNode: AddNodeType,
) {

    /**
     * Getting all document nodes
     *
     * @return document file nodes
     */
    suspend operator fun invoke(): List<TypedFileNode> =
        documentSectionRepository.getAllDocuments(getCloudSortOrder()).map {
            addTypedNode(it)
        }.filterIsInstance<TypedFileNode>()
}