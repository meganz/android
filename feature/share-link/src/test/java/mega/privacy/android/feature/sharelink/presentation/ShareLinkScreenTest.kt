package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sharelink.presentation.component.SHARE_LINK_DETAILS_TAG
import mega.privacy.android.feature.sharelink.presentation.component.SHARE_LINK_KEY_COPY_TAG
import mega.privacy.android.feature.sharelink.presentation.component.SHARE_LINK_KEY_DETAILS_TAG
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShareLinkScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val data = ShareLinkUiState.Data(
        nodeLinks = listOf(
            ShareLinkNodeItem(
                handle = 1L,
                name = "Presentation.pdf",
                isFolder = false,
                iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                sizeInBytes = 10L * 1024 * 1024,
                modificationTime = 1_749_000_000L,
                childFolderCount = null,
                childFileCount = null,
                link = "https://mega.nz/file/abc123#decryptionKey",
                linkWithoutKey = "https://mega.nz/file/abc123",
                key = "decryptionKey",
            ),
        ),
        accountType = null,
    )

    private val multiNodeData = ShareLinkUiState.Data(
        nodeLinks = listOf(
            ShareLinkNodeItem(
                handle = 1L,
                name = "Documents",
                isFolder = true,
                iconRes = iconPackR.drawable.ic_folder_medium_solid,
                sizeInBytes = null,
                modificationTime = null,
                childFolderCount = 6,
                childFileCount = 12,
                link = "https://mega.nz/folder/abc123#folderKey",
                linkWithoutKey = "https://mega.nz/folder/abc123",
                key = "folderKey",
            ),
            ShareLinkNodeItem(
                handle = 2L,
                name = "Presentation.pdf",
                isFolder = false,
                iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                sizeInBytes = 10L * 1024 * 1024,
                modificationTime = 1_749_000_000L,
                childFolderCount = null,
                childFileCount = null,
                link = "https://mega.nz/file/def456#fileKey",
                linkWithoutKey = "https://mega.nz/file/def456",
                key = "fileKey",
            ),
        ),
        accountType = null,
    )

    @Test
    fun `test that every shared node and one access banner are displayed in the multi-node state`() {
        setContent(uiState = multiNodeData)

        composeRule.onNodeWithTag(SHARE_LINK_MULTI_NODE_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Documents").assertIsDisplayed()
        composeRule.onNodeWithText("Presentation.pdf").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_ACCESS_BANNER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that folder content info is displayed for a folder node in the multi-node state`() {
        setContent(uiState = multiNodeData)

        val folderInfo = context.resources.getQuantityString(
            sharedR.plurals.info_num_folders_and_files, 6, 6,
        ) + context.resources.getQuantityString(sharedR.plurals.info_num_files, 12, 12)
        composeRule.onNodeWithText(folderInfo).assertIsDisplayed()
    }

    @Test
    fun `test that tapping a node's copy icon copies that node's link to the clipboard`() {
        val clipboard = FakeClipboard()
        setContent(uiState = multiNodeData, clipboard = clipboard)

        composeRule.onAllNodesWithContentDescription(context.getString(sharedR.string.general_copy))[0]
            .performClick()
        composeRule.waitForIdle()

        assertThat(clipboard.clipEntry?.clipData?.getItemAt(0)?.text)
            .isEqualTo(multiNodeData.nodeLinks[0].link)
    }

    @Test
    fun `test that the node header, link access banner and link field are displayed in the Data state`() {
        setContent(uiState = data)

        composeRule.onNodeWithText("Presentation.pdf").assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_ACCESS_BANNER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_DETAILS_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_SHARE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the settings action is shown for a single node`() {
        setContent(uiState = data)

        composeRule.onNodeWithTag(ShareLinkSettingsAction.testTag, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that the settings action is hidden for multiple nodes`() {
        val multiNode = data.copy(nodeLinks = data.nodeLinks + data.primary.copy(handle = 2L))

        setContent(uiState = multiNode)

        composeRule.onNodeWithTag(ShareLinkSettingsAction.testTag, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that the loading placeholder is displayed in the Loading state`() {
        setContent(uiState = ShareLinkUiState.Loading)

        composeRule.onNodeWithTag(SHARE_LINK_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the error message is displayed in the Error state`() {
        setContent(uiState = ShareLinkUiState.Error)

        composeRule.onNodeWithTag(SHARE_LINK_ERROR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the share button invokes onShareLink`() {
        var shared = false
        setContent(uiState = data, onShareLink = { shared = true })

        composeRule.onNodeWithTag(SHARE_LINK_SHARE_BUTTON_TAG).performClick()

        assertThat(shared).isTrue()
    }

    @Test
    fun `test that tapping the copy icon invokes onCopyLink`() {
        var copied = false
        setContent(uiState = data, onCopyLink = { copied = true })

        composeRule.onNodeWithContentDescription(context.getString(sharedR.string.general_copy))
            .performClick()

        assertThat(copied).isTrue()
    }

    @Test
    fun `test that tapping the copy icon copies the link to the clipboard`() {
        val clipboard = FakeClipboard()
        setContent(uiState = data, clipboard = clipboard)

        composeRule.onNodeWithContentDescription(context.getString(sharedR.string.general_copy))
            .performClick()
        composeRule.waitForIdle()

        assertThat(clipboard.clipEntry?.clipData?.getItemAt(0)?.text).isEqualTo(data.primary.link)
    }

    @Test
    fun `test that the key card and key-less link are displayed when the key is separate`() {
        setContent(uiState = data.copy(isKeySeparate = true))

        composeRule.onNodeWithText("https://mega.nz/file/abc123").assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_LINK_KEY_DETAILS_TAG).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("decryptionKey").assertIsDisplayed()
    }

    @Test
    fun `test that the key card is hidden when the key is not separate`() {
        setContent(uiState = data)

        composeRule.onNodeWithTag(SHARE_LINK_KEY_DETAILS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the access banner mentions the key when the key is separate`() {
        setContent(uiState = data.copy(isKeySeparate = true))

        composeRule.onNodeWithText(
            context.resources.getQuantityString(
                sharedR.plurals.share_link_access_banner_description_with_key,
                data.handles.size,
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that tapping the key copy icon invokes onCopyKey`() {
        var copied = false
        setContent(uiState = data.copy(isKeySeparate = true), onCopyKey = { copied = true })

        composeRule.onNodeWithTag(SHARE_LINK_KEY_COPY_TAG).performScrollTo().performClick()

        assertThat(copied).isTrue()
    }

    @Test
    fun `test that tapping the key copy icon copies the key to the clipboard`() {
        val clipboard = FakeClipboard()
        setContent(uiState = data.copy(isKeySeparate = true), clipboard = clipboard)

        composeRule.onNodeWithTag(SHARE_LINK_KEY_COPY_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(clipboard.clipEntry?.clipData?.getItemAt(0)?.text).isEqualTo(data.primary.key)
    }

    private fun setContent(
        uiState: ShareLinkUiState,
        onBack: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
        onShareLink: () -> Unit = {},
        onCopyLink: () -> Unit = {},
        onCopyKey: () -> Unit = {},
        clipboard: Clipboard = FakeClipboard(),
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                ShareLinkScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                    onShareLink = onShareLink,
                    onCopyLink = onCopyLink,
                    onCopyKey = onCopyKey,
                )
            }
        }
    }

    private class FakeClipboard : Clipboard {
        var clipEntry: ClipEntry? = null
            private set

        override suspend fun getClipEntry(): ClipEntry? = clipEntry

        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            this.clipEntry = clipEntry
        }

        override val nativeClipboard: NativeClipboard
            get() = throw UnsupportedOperationException("Not used in tests")
    }
}
