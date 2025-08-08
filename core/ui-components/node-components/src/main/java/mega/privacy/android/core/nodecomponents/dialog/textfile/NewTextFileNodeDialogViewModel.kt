package mega.privacy.android.core.nodecomponents.dialog.textfile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.node.ValidateNodeNameUseCase
import javax.inject.Inject

/**
 * ViewModel for the new text file dialog.
 * Handles validation and checks for existing file names.
 */
@HiltViewModel
class NewTextFileNodeDialogViewModel @Inject constructor(
    private val validateNodeNameUseCase: ValidateNodeNameUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
) : ViewModel() {

    /**
     * Creates a text file after validation.
     *
     * @param fileName The file name to create
     * @param parentNodeId The parent node ID where the file will be created
     */
    suspend fun createTextFile(
        fileName: String,
        parentNodeId: NodeId,
    ) = runCatching {
        val trimmedFileName = fileName.trim()
        val parentOrRootNodeId =
            if (parentNodeId.longValue != -1L) parentNodeId else getRootNodeUseCase()?.id
                ?: throw IllegalStateException("Root node not found")
        validateNodeNameUseCase(trimmedFileName, parentOrRootNodeId)
        parentOrRootNodeId to trimmedFileName
    }
}