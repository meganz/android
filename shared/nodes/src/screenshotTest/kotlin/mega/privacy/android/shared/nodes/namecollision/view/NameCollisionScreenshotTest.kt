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
 * Screenshot tests for [NameCollisionScreen] covering the golden matrix: file vs folder collision,
 * upload/copy/move resolution, and single vs "apply to all" states.
 */
class NameCollisionScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadReplace() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD, isFileVersioningEnabled = false),
                pendingFileCollisions = 0,
                hasPendingCollisions = false,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileUploadUpdateApplyForAll() {
        Preview(
            uiState = uiState(
                currentCollision = fileCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD, isFileVersioningEnabled = true),
                pendingFileCollisions = 5,
                hasPendingCollisions = true,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileCopyReplace() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = true, type = NodeNameCollisionType.COPY),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.COPY, isFileVersioningEnabled = false),
                pendingFileCollisions = 0,
                hasPendingCollisions = false,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FileMoveReplace() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = true, type = NodeNameCollisionType.MOVE),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.MOVE, isFileVersioningEnabled = false),
                pendingFileCollisions = 0,
                hasPendingCollisions = false,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FolderUploadMerge() {
        Preview(
            uiState = uiState(
                currentCollision = uploadFolderCollision(),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.UPLOAD, isFileVersioningEnabled = false, isFile = false),
                pendingFolderCollisions = 0,
                hasPendingCollisions = false,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FolderCopyMerge() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = false, type = NodeNameCollisionType.COPY),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.COPY, isFileVersioningEnabled = false, isFile = false),
                pendingFolderCollisions = 0,
                hasPendingCollisions = false,
            )
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun FolderMoveMergeApplyForAll() {
        Preview(
            uiState = uiState(
                currentCollision = nodeCollision(isFile = false, type = NodeNameCollisionType.MOVE),
                fileVersioningInfo = fileVersioningInfo(NameCollisionType.MOVE, isFileVersioningEnabled = false, isFile = false),
                pendingFolderCollisions = 3,
                hasPendingCollisions = true,
            )
        )
    }

    @Composable
    private fun Preview(uiState: NameCollisionUiState) {
        AndroidThemeForPreviews {
            NameCollisionScreen(
                uiState = uiState,
                onReplaceUpdateMerge = {},
                onCancel = {},
                onRename = {},
                onLearnMore = {},
                onBack = {},
            )
        }
    }

    private fun uiState(
        currentCollision: NodeNameCollisionResult,
        fileVersioningInfo: FileVersioningInfo,
        pendingFileCollisions: Int = 0,
        pendingFolderCollisions: Int = 0,
        hasPendingCollisions: Boolean,
    ) = NameCollisionUiState(
        currentCollision = currentCollision,
        fileVersioningInfo = fileVersioningInfo,
        isCopyToOrigin = false,
        pendingFileCollisions = pendingFileCollisions,
        pendingFolderCollisions = pendingFolderCollisions,
        hasPendingCollisions = hasPendingCollisions,
        uploadedFilesCount = 0,
        uploadEvent = consumed(),
        outcome = consumed(),
    )

    private fun fileVersioningInfo(
        type: NameCollisionType,
        isFileVersioningEnabled: Boolean,
        isFile: Boolean = true,
    ) = FileVersioningInfo(
        isFileVersioningEnabled = isFileVersioningEnabled,
        nameCollisionType = type,
        isFile = isFile,
        canReplaceOrUpdate = true,
    )

    private fun fileCollision(): NodeNameCollisionResult = NodeNameCollisionResult(
        nameCollision = FileNameCollision(
            collisionHandle = 1L,
            name = "IMG000012.jpeg",
            size = 57_344L,
            lastModified = MODIFIED_SECONDS,
            parentHandle = 10L,
            isFile = true,
            renameName = "IMG000012 (1).jpeg",
            pitagTrigger = PitagTrigger.NotApplicable,
            path = UriPath("file:///storage/emulated/0/DCIM/IMG000012.jpeg"),
        ),
        collisionName = "IMG000012.jpeg",
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
