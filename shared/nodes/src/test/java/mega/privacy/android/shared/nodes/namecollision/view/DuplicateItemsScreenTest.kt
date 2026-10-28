package mega.privacy.android.shared.nodes.namecollision.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.shared.nodes.namecollision.FileVersioningInfo
import mega.privacy.android.shared.nodes.namecollision.NameCollisionType
import mega.privacy.android.shared.nodes.namecollision.NameCollisionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class DuplicateItemsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that the replace option says the file is overwritten when a file is moved with file versioning enabled`() {
        setContent(NodeNameCollisionType.MOVE, NameCollisionType.MOVE)

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.name_collision_option_replace_file_subtitle)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that the replace option says the file is saved as a previous version when a file is copied with file versioning enabled`() {
        setContent(NodeNameCollisionType.COPY, NameCollisionType.COPY)

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.name_collision_option_replace_file_versioning_subtitle)
        ).assertIsDisplayed()
    }

    private fun setContent(
        nodeCollisionType: NodeNameCollisionType,
        collisionType: NameCollisionType,
    ) {
        composeTestRule.setContent {
            DuplicateItemsScreenContent(
                uiState = NameCollisionUiState(
                    currentCollision = fileCollision(nodeCollisionType),
                    fileVersioningInfo = FileVersioningInfo(
                        isFileVersioningEnabled = true,
                        nameCollisionType = collisionType,
                        isFile = true,
                        canReplaceOrUpdate = true,
                    ),
                    isCopyToOrigin = false,
                    pendingFileCollisions = 0,
                    pendingFolderCollisions = 0,
                    currentCollisionIndex = 1,
                    currentTypeCollisionCount = 1,
                    hasPendingCollisions = false,
                    uploadedFilesCount = 0,
                    uploadEvent = consumed(),
                    outcome = consumed(),
                    dismissEvent = consumed,
                ),
                selectedOption = null,
                onOptionSelected = {},
                applyToAll = false,
                onApplyToAllChanged = {},
                onPrimaryClick = {},
                onBack = {},
            )
        }
    }

    private fun fileCollision(type: NodeNameCollisionType) = NodeNameCollisionResult(
        nameCollision = NodeNameCollision.Default(
            collisionHandle = 2L,
            nodeHandle = 3L,
            name = "Report.pdf",
            size = 120_000L,
            childFolderCount = 0,
            childFileCount = 0,
            lastModified = 1_600_000_000L,
            parentHandle = 10L,
            isFile = true,
            renameName = "Report (1).pdf",
            type = type,
        ),
        collisionName = "Report.pdf",
        collisionSize = 98_000L,
        collisionFolderContent = null,
        collisionLastModified = 1_600_000_000L,
        collisionThumbnail = null,
        thumbnail = null,
        choice = null,
    )
}
