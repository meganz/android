package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import javax.inject.Inject

/** Cache folder name for text editor temp files (matches data layer constant). */
private const val TEXT_EDITOR_TEMP_FOLDER = "tempMEGA"

/**
 * Use case to save text content from the text editor (Edit or Create mode).
 * Resolves parent handle, gets unique file name, writes to temp file via [FileSystemRepository].
 * On success returns [TextEditorSaveResult.UploadRequired] so the ViewModel can trigger the transfer event.
 */
class SaveTextContentForTextEditorUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Save text for the given parameters.
     * @return [TextEditorSaveResult]. Throws on error; caller (e.g. ViewModel) should catch and map to UI state.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        nodeSourceType: Int,
        text: String,
        fileName: String,
        mode: TextEditorMode,
        fromHome: Boolean = false,
    ): TextEditorSaveResult = withContext(ioDispatcher) {
        val parentHandle = resolveParentHandle(nodeHandle, mode)
            ?: throw IllegalStateException("Could not resolve parent handle")
        val baseFileName = fileName.ifEmpty { "untitled.txt" }
        val nameCollision = FileNameCollision(
            collisionHandle = 0L,
            name = baseFileName,
            size = 0L,
            lastModified = System.currentTimeMillis(),
            parentHandle = parentHandle,
            path = UriPath(""),
            pitagTrigger = PitagTrigger.NotApplicable,
        )
        val uniqueFileName = getNodeNameCollisionRenameNameUseCase(nameCollision)
        val tempFile = getCacheFileUseCase(TEXT_EDITOR_TEMP_FOLDER, uniqueFileName)
            ?: throw IllegalStateException("Cannot get temp file")
        fileSystemRepository.writeTextToPath(tempFile.absolutePath, text)
        TextEditorSaveResult.UploadRequired(
            tempPath = tempFile.absolutePath,
            parentHandle = parentHandle,
            isEditMode = mode == TextEditorMode.Edit,
            fromHome = fromHome,
        )
    }

    private suspend fun resolveParentHandle(
        nodeHandle: Long,
        mode: TextEditorMode,
    ): Long? = when (mode) {
        TextEditorMode.Edit -> {
            val node = getNodeByIdUseCase(NodeId(nodeHandle)) ?: return null
            node.parentId.longValue
        }
        TextEditorMode.Create -> {
            if (nodeHandle == 0L || nodeHandle == -1L) {
                getRootNodeUseCase()?.id?.longValue
            } else {
                getNodeByIdUseCase(NodeId(nodeHandle))?.id?.longValue
            }
        }
        TextEditorMode.View -> null
    }
}
