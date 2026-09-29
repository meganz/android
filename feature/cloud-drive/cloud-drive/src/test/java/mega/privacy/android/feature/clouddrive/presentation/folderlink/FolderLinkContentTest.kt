package mega.privacy.android.feature.clouddrive.presentation.folderlink

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.view.GUEST_BANNER_TAG
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.DECRYPTION_KEY_DIALOG_TAG
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkAction
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.nodes.model.NodeViewItem
import mega.privacy.android.shared.nodes.selection.NodeSelection
import mega.privacy.android.shared.nodes.selection.rememberNodeSelection
import mega.privacy.android.shared.nodes.selection.rememberNodeSelectionState
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FolderLinkContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var selection: NodeSelection<TypedNode>

    @Test
    fun `test that long clicking an item selects it`() {
        setupComposeContent(
            uiState = loadedState(createNodeViewItem(id = 1L, name = "Folder 1")),
        )

        composeRule.onNodeWithText("Folder 1").performTouchInput { longClick() }

        composeRule.runOnIdle {
            assertThat(selection.selectedNodes.map { it.id }).containsExactly(NodeId(1L))
        }
    }

    @Test
    fun `test that clicking an item toggles its selection instead of opening it when in selection mode`() {
        val onAction = mock<(FolderLinkAction) -> Unit>()
        setupComposeContent(
            uiState = loadedState(
                createNodeViewItem(id = 1L, name = "Folder 1"),
                createNodeViewItem(id = 2L, name = "Folder 2"),
            ),
            selectedIds = setOf(NodeId(1L)),
            onAction = onAction,
        )

        composeRule.onNodeWithText("Folder 2").performClick()

        composeRule.runOnIdle {
            assertThat(selection.selectedNodes.map { it.id })
                .containsExactly(NodeId(1L), NodeId(2L))
        }
        verify(onAction, never()).invoke(any<FolderLinkAction.ItemClicked>())
    }

    @Test
    fun `test that clicking an item dispatches ItemClicked when not in selection mode`() {
        val onAction = mock<(FolderLinkAction) -> Unit>()
        val item = createNodeViewItem(id = 1L, name = "Folder 1")
        setupComposeContent(uiState = loadedState(item), onAction = onAction)

        composeRule.onNodeWithText("Folder 1").performClick()

        verify(onAction).invoke(FolderLinkAction.ItemClicked(item.node))
        composeRule.runOnIdle { assertThat(selection.isInSelectionMode).isFalse() }
    }

    @Test
    fun `test that loading skeleton is displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag(FOLDER_LINK_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that decryption key required text is displayed`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/folder/abc",
                    isKeyIncorrect = false,
                )
            )
        )

        composeRule.onNodeWithTag(DECRYPTION_KEY_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that invalid decryption key text is displayed when key is incorrect`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/folder/abc",
                    isKeyIncorrect = true,
                )
            )
        )

        composeRule.onNodeWithTag(DECRYPTION_KEY_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that expired text is displayed when state is Expired`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Expired,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_EXPIRED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that unavailable text is displayed when state is Unavailable`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Unavailable,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_UNAVAILABLE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when Loaded with no items`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loaded,
                items = emptyList(),
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that loading tag is not displayed when state is Expired`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Expired,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_LOADING_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that expired tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_EXPIRED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that unavailable tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_UNAVAILABLE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that guest banner is not displayed for authenticated user`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loaded,
                hasCredentials = true,
            )
        )

        composeRule.onNodeWithTag(GUEST_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that guest banner is not displayed when banner has been dismissed`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loaded,
                hasCredentials = false,
                isGuestBannerDismissed = true,
            )
        )

        composeRule.onNodeWithTag(GUEST_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that guest banner is not displayed when content is still Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
                hasCredentials = false,
            )
        )

        composeRule.onNodeWithTag(GUEST_BANNER_TAG).assertDoesNotExist()
    }

    private fun loadedState(vararg items: NodeViewItem<TypedNode>) = FolderLinkUiState(
        contentState = FolderLinkContentState.Loaded,
        items = items.toList(),
    )

    private fun createNodeViewItem(id: Long, name: String): NodeViewItem<TypedNode> {
        val node = mock<TypedFolderNode> {
            on { this.id } doReturn NodeId(id)
            on { this.name } doReturn name
        }
        return NodeViewItem(
            node = node,
            title = LocalizedText.Literal(name),
            iconRes = iconPackR.drawable.ic_folder_medium_solid,
        )
    }

    private fun setupComposeContent(
        uiState: FolderLinkUiState = FolderLinkUiState(),
        selectedIds: Set<NodeId> = emptySet(),
        onAction: (FolderLinkAction) -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                selection = rememberNodeSelection(
                    items = uiState.items,
                    state = rememberNodeSelectionState(initialSelectedIds = selectedIds),
                )
                FolderLinkContent(
                    uiState = uiState,
                    selection = selection,
                    isListView = true,
                    spanCount = 2,
                    onNavigate = {},
                    onAction = onAction,
                    onMediaDiscoveryClicked = {},
                    onCreateAccountClicked = {},
                    onLoginClicked = {},
                )
            }
        }
    }
}
