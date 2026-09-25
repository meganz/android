package mega.privacy.android.domain.usecase.texteditor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.texteditor.TextEditorSaveResult
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.GetNodeAccessUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionRenameNameUseCase
import javax.inject.Inject

/**
 * Use case to save text content from the text editor (Edit or Create mode).
 * Resolves parent handle, picks the file name, writes to temp file via [FileSystemRepository].
 * On success returns [TextEditorSaveResult.UploadRequired] so the ViewModel can trigger the transfer event.
 *
 * Create mode always uses a unique name. In Edit mode the node access level decides, because only a user who
 * may create a version can overwrite the file — reusing the name without that right leaves two files with the
 * same name in the same folder:
 *
 * | Access                   | File name                       |
 * |--------------------------|---------------------------------|
 * | READ                     | unreachable — see below         |
 * | READWRITE                | unique name — `(1)`, `(2)`, ... |
 * | FULL, OWNER              | original name — overwrites      |
 * | UNKNOWN, or unresolvable | original name — safe default    |
 *
 * READ is grouped with READWRITE as a defensive fallback only; it does not mean read-only nodes can be
 * saved. Every entry point into Edit mode already requires OWNER, READWRITE or FULL, so a read-only node
 * never reaches this use case, and the API would reject the upload if it did.
 */
class SaveTextContentForTextEditorUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getRootNodeUseCase: GetRootNodeUseCase,
    private val getNodeNameCollisionRenameNameUseCase: GetNodeNameCollisionRenameNameUseCase,
    private val getCacheFileUseCase: GetCacheFileUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val getNodeAccessUseCase: GetNodeAccessUseCase,
) {

    /**
     * Save text for the given parameters.
     * @param fromHome Legacy parameter: when true (e.g. new file from Home), forwarded to upload handling. The Compose editor passes this from navigation args when applicable.
     * @return [TextEditorSaveResult]. Throws on error; caller (e.g. ViewModel) should catch and map to UI state.
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        text: String,
        fileName: String,
        mode: TextEditorMode,
        fromHome: Boolean = false,
    ): TextEditorSaveResult = withContext(ioDispatcher) {
        val parentHandle = resolveParentHandle(nodeHandle, mode)
            ?: throw IllegalStateException("Could not resolve parent handle")
        val baseFileName = fileName.ifEmpty { "untitled.txt" }
        val keepOriginalName = mode == TextEditorMode.Edit && canCreateVersion(nodeHandle)
        val fileNameToUse = when {
            keepOriginalName -> baseFileName
            else -> getUniqueFileName(baseFileName, parentHandle)
        }
        var tempFile = getCacheFileUseCase(TEXT_EDITOR_TEMP_FOLDER, fileNameToUse)
            ?: throw IllegalStateException("Cannot get temp file")
        if (tempFile.exists() && tempFile.isDirectory) {
            if (keepOriginalName) {
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

    /**
     * Only a known insufficient permission blocks versioning. An unresolvable access level counts as
     * sufficient so an unexpected SDK answer cannot turn a normal edit into a renamed copy.
     * READ cannot reach Edit mode at all; it is listed to keep the branch exhaustive.
     */
    private suspend fun canCreateVersion(nodeHandle: Long): Boolean =
        when (getNodeAccessUseCase(NodeId(nodeHandle))) {
            AccessPermission.READ,
            AccessPermission.READWRITE,
                -> false

            AccessPermission.FULL,
            AccessPermission.OWNER,
            AccessPermission.UNKNOWN,
            null,
                -> true
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
