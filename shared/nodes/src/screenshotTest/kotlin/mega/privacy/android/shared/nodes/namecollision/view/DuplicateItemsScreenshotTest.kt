package mega.privacy.android.shared.nodes.namecollision.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.shared.nodes.namecollision.FileVersioningInfo
import mega.privacy.android.shared.nodes.namecollision.NameCollisionType
import mega.privacy.android.shared.nodes.namecollision.NameCollisionUiState

/**
 * Screenshot tests for [DuplicateItemsScreenContent] covering the golden matrix: file vs folder
 * duplicate, upload/copy/move/import action, file versioning, the replace option being
 * unavailable, a long name, and the single vs "apply to all" states with and without a selection.
 */
class DuplicateItemsScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadNoSelection() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadReplaceSelectedApplyToAll() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD),
                pendingFileCollisions = 2,
                currentCollisionIndex = 1,
                currentTypeCollisionCount = 3,
            ),
            selectedOption = DuplicateItemOption.Replace,
            applyToAll = true,
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadVersioningOn() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(
                    NameCollisionType.UPLOAD,
                    isFileVersioningEnabled = true,
                ),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadMultipleApplyToAll() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD),
                pendingFileCollisions = 4,
                currentCollisionIndex = 1,
                currentTypeCollisionCount = 5,
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileCopy() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = true, type = NodeNameCollisionType.COPY),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.COPY),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileMove() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = true, type = NodeNameCollisionType.MOVE),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.MOVE),
            ),
            selectedOption = DuplicateItemOption.KeepBoth,
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileReplaceUnavailable() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = true, type = NodeNameCollisionType.COPY),
                fileVersioningInfo = fileVersioningInfo(
                    NameCollisionType.COPY,
                    canReplaceOrUpdate = false,
                ),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileLongName() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(
                    name = "Quarterly financial report for the board meeting 2026 final reviewed copy.pdf",
                    renameName = "Quarterly financial report for the board meeting 2026 final reviewed copy (1).pdf",
                ),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileImportFromChat() {
        Preview(
            uiState = uiState(
                currentCollision = chatCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.COPY),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FolderUploadSingle() {
        Preview(
            uiState = uiState(
                currentCollision = uploadFolderCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD, isFile = false),
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FolderCopyMultipleApplyToAll() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = false, type = NodeNameCollisionType.COPY),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.COPY, isFile = false),
                pendingFolderCollisions = 1,
                currentCollisionIndex = 2,
                currentTypeCollisionCount = 3,
            ),
            applyToAll = true,
        )
    }

    @Composable
    private fun Preview(
        uiState: NameCollisionUiState,
        selectedOption: DuplicateItemOption? = null,
        applyToAll: Boolean = false,
    ) {
        AndroidThemeForPreviews {
            DuplicateItemsScreenContent(
                uiState = uiState,
                selectedOption = selectedOption,
                onOptionSelected = {},
                applyToAll = applyToAll,
                onApplyToAllChanged = {},
                onPrimaryClick = {},
                onBack = {},
            )
        }
    }

    private fun uiState(
        currentCollision: NodeNameCollisionResult,
        fileVersioningInfo: FileVersioningInfo,
        pendingFileCollisions: Int = 0,
        pendingFolderCollisions: Int = 0,
        currentCollisionIndex: Int = 1,
        currentTypeCollisionCount: Int = 1,
    ) = NameCollisionUiState(
        currentCollision = currentCollision,
        fileVersioningInfo = fileVersioningInfo,
        isCopyToOrigin = false,
        pendingFileCollisions = pendingFileCollisions,
        pendingFolderCollisions = pendingFolderCollisions,
        currentCollisionIndex = currentCollisionIndex,
        currentTypeCollisionCount = currentTypeCollisionCount,
        hasPendingCollisions = pendingFileCollisions + pendingFolderCollisions > 0,
        uploadedFilesCount = 0,
        uploadEvent = consumed(),
        outcome = consumed(),
        dismissEvent = consumed,
    )

    private fun fileVersioningInfo(
        type: NameCollisionType,
        isFileVersioningEnabled: Boolean = false,
        isFile: Boolean = true,
        canReplaceOrUpdate: Boolean = true,
    ) = FileVersioningInfo(
        isFileVersioningEnabled = isFileVersioningEnabled,
        nameCollisionType = type,
        isFile = isFile,
        canReplaceOrUpdate = canReplaceOrUpdate,
    )

    private fun fileCollision(
        name: String = "IMG000012.jpeg",
        renameName: String = "IMG000012 (1).jpeg",
    ): NodeNameCollisionResult = NodeNameCollisionResult(
        nameCollision = FileNameCollision(
            collisionHandle = 1L,
            name = name,
            size = 57_344L,
            lastModified = MODIFIED_SECONDS,
            parentHandle = 10L,
            isFile = true,
            renameName = renameName,
            pitagTrigger = PitagTrigger.NotApplicable,
            path = UriPath("file:///storage/emulated/0/DCIM/$name"),
        ),
        collisionName = name,
        collisionSize = 40_960L,
        collisionFolderContent = null,
        collisionLastModified = MODIFIED_SECONDS,
        collisionThumbnail = null,
        thumbnail = null,
        choice = null,
    )

    private fun nodeCollision(
        isFile: Boolean,
        type: NodeNameCollisionType,
    ): NodeNameCollisionResult = NodeNameCollisionResult(
        nameCollision = NodeNameCollision.Default(
            collisionHandle = 2L,
            nodeHandle = 3L,
            name = if (isFile) "Report.pdf" else "Documents",
            size = if (isFile) 120_000L else 0L,
            childFolderCount = if (isFile) 0 else 2,
            childFileCount = if (isFile) 0 else 5,
            lastModified = MODIFIED_SECONDS,
            parentHandle = 10L,
            isFile = isFile,
            renameName = if (isFile) "Report (1).pdf" else "Documents (1)",
            type = type,
        ),
        collisionName = if (isFile) "Report.pdf" else "Documents",
        collisionSize = if (isFile) 98_000L else null,
        collisionFolderContent = if (isFile) null else FolderTreeInfo(
            numberOfFiles = 8,
            numberOfFolders = 3,
            totalCurrentSizeInBytes = 1_048_576L,
            numberOfVersions = 0,
            sizeOfPreviousVersionsInBytes = 0L,
        ),
        collisionLastModified = MODIFIED_SECONDS,
        collisionThumbnail = null,
        thumbnail = null,
        choice = null,
    )

    private fun chatCollision(): NodeNameCollisionResult = NodeNameCollisionResult(
        nameCollision = NodeNameCollision.Chat(
            collisionHandle = 5L,
            nodeHandle = 6L,
            name = "Invoice.pdf",
            size = 64_000L,
            childFolderCount = 0,
            childFileCount = 0,
            lastModified = MODIFIED_SECONDS,
            parentHandle = 10L,
            isFile = true,
            renameName = "Invoice (1).pdf",
            chatId = 7L,
            messageId = 8L,
        ),
        collisionName = "Invoice.pdf",
        collisionSize = 60_000L,
        collisionFolderContent = null,
        collisionLastModified = MODIFIED_SECONDS,
        collisionThumbnail = null,
        thumbnail = null,
        choice = null,
    )

    private fun uploadFolderCollision(): NodeNameCollisionResult = NodeNameCollisionResult(
        nameCollision = FileNameCollision(
            collisionHandle = 4L,
            name = "Holiday Photos",
            size = 0L,
            childFolderCount = 2,
            childFileCount = 6,
            lastModified = MODIFIED_SECONDS,
            parentHandle = 10L,
            isFile = false,
            renameName = "Holiday Photos (1)",
            pitagTrigger = PitagTrigger.NotApplicable,
            path = UriPath("file:///storage/emulated/0/DCIM/Holiday Photos"),
        ),
        collisionName = "Holiday Photos",
        collisionSize = null,
        collisionFolderContent = FolderTreeInfo(
            numberOfFiles = 10,
            numberOfFolders = 4,
            totalCurrentSizeInBytes = 2_097_152L,
            numberOfVersions = 0,
            sizeOfPreviousVersionsInBytes = 0L,
        ),
        collisionLastModified = MODIFIED_SECONDS,
        collisionThumbnail = null,
        thumbnail = null,
        choice = null,
    )

    private companion object {
        private const val MODIFIED_SECONDS = 1_600_000_000L
    }
}
