package mega.privacy.android.domain.usecase.documentsection

import mega.privacy.android.domain.entity.node.TypedFileNode
import javax.inject.Inject

/**
 * The use case for getting all document nodes
 */
class GetAllDocumentsUseCase @Inject constructor() {

    /**
     * Getting all document nodes
     */
    suspend operator fun invoke(): List<TypedFileNode> =
        //TODO add the logic to get all documents
        emptyList()
}