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

/**
 * Use case to save text content from the text editor (Edit or Create mode).
 * Resolves parent handle, gets file name (unique when from shared folder or Create; original when Edit in Cloud Drive), writes to temp file via [FileSystemRepository].
 * On success returns [TextEditorSaveResult.UploadRequired] so the ViewModel can trigger the transfer event.
 *
 * When [isFromSharedFolder] is true (incoming/outgoing shares or links), the save always uses a unique name (1)(2) so a new file is created.
 * When false and Edit mode, the original file name is used so the upload overwrites the same file.
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
     * @param isFromSharedFolder When true (e.g. incoming/outgoing shares), save as new file with (1)(2) naming. When false and Edit mode, overwrite the same file.
     * @param fromHome Legacy parameter kept for backward compatibility with the legacy text editor. The Compose editor always passes false; use [isFromSharedFolder] instead.
     * @return [TextEditorSaveResult]. Throws on error; caller (e.g. ViewModel) should catch and map to UI state.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        text: String,
        fileName: String,
        mode: TextEditorMode,
        fromHome: Boolean = false,
        isFromSharedFolder: Boolean = false,
    ): TextEditorSaveResult = withContext(ioDispatcher) {
        val parentHandle = resolveParentHandle(nodeHandle, mode)
            ?: throw IllegalStateException("Could not resolve parent handle")
        val baseFileName = fileName.ifEmpty { "untitled.txt" }
        val fileNameToUse = when {
            mode == TextEditorMode.Edit && !isFromSharedFolder -> baseFileName
            else -> getUniqueFileName(baseFileName, parentHandle)
        }
        var tempFile = getCacheFileUseCase(TEXT_EDITOR_TEMP_FOLDER, fileNameToUse)
            ?: throw IllegalStateException("Cannot get temp file")
        if (tempFile.exists() && tempFile.isDirectory) {
            if (mode == TextEditorMode.Edit && !isFromSharedFolder) {
                fileSystemRepository.deleteFolderAndItsFiles(tempFile.absolutePath)
            } else {
                val fallbackName = getUniqueFileName(fileNameToUse, parentHandle)
                tempFile = getCacheFileUseCase(TEXT_EDITOR_TEMP_FOLDER, fallbackName)
                    ?: throw IllegalStateException("Cannot get temp file")
            }
        }
        fileSystemRepository.writeTextToPath(tempFile.absolutePath, text)
        TextEditorSaveResult.UploadRequired(
            tempPath = tempFile.absolutePath,
            parentHandle = parentHandle,
            isEditMode = mode == TextEditorMode.Edit,
            fromHome = fromHome,
        )
    }

    private suspend fun getUniqueFileName(name: String, parentHandle: Long): String =
        getNodeNameCollisionRenameNameUseCase(
            FileNameCollision(
                collisionHandle = 0L,
                name = name,
                size = 0L,
                lastModified = System.currentTimeMillis(),
                parentHandle = parentHandle,
                path = UriPath(""),
                pitagTrigger = PitagTrigger.NotApplicable,
            )
        )

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
