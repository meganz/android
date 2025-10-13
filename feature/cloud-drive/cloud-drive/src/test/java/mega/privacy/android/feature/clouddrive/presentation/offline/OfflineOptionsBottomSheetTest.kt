package mega.privacy.android.feature.clouddrive.presentation.offline

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class OfflineOptionsBottomSheetTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that OfflineOptionsBottomSheet displays file information correctly`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false,
            size = 1024L
        )
        setupComposeContent(offlineFileInformation)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_NODE_LIST_VIEW_ITEM).assertIsDisplayed()
        composeRule.onNodeWithText("test_file.pdf").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays all menu items for file when online`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(offlineFileInformation, isOnline = true)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_INFO_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays all menu items for file when offline`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(offlineFileInformation, isOnline = false)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_INFO_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays correct menu items for folder when online`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_folder",
            isFolder = true
        )
        setupComposeContent(offlineFileInformation, isOnline = true)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_INFO_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).assertIsNotDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays correct menu items for folder when offline`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_folder",
            isFolder = true
        )
        setupComposeContent(offlineFileInformation, isOnline = false)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_INFO_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).assertIsNotDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).assertIsNotDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays correct menu item texts`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(offlineFileInformation, isOnline = true)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(sharedResR.string.general_info))
            .assertIsDisplayed()
        composeRule.onNodeWithText("Open with").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(sharedResR.string.general_share))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(sharedResR.string.general_save_to_device))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.offline_screen_selection_menu_remove_from_offline))
            .assertIsDisplayed()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet calls onShareOfflineFile when share menu item is clicked`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            isOnline = true,
            onShareOfflineFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).performClick()
        verify(mockCallback).invoke()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet calls onSaveOfflineFileToDevice when save to device menu item is clicked`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            onSaveOfflineFileToDevice = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SAVE_TO_DEVICE_MENU_ITEM).performClick()
        verify(mockCallback).invoke()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet calls onDeleteOfflineFile when delete menu item is clicked`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            onDeleteOfflineFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_DELETE_MENU_ITEM).performClick()
        verify(mockCallback).invoke()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet calls onOpenOfflineFile when info menu item is clicked`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            onOpenOfflineFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_INFO_MENU_ITEM).performClick()
        verify(mockCallback).invoke()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet calls onOpenWithFile when open with menu item is clicked`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_file.pdf",
            isFolder = false
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            onOpenWithFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).performClick()
        verify(mockCallback).invoke()
    }

    @Test
    fun `test that OfflineOptionsBottomSheet does not call onOpenWithFile for folders`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_folder",
            isFolder = true
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            onOpenWithFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_OPEN_WITH_MENU_ITEM).assertIsNotDisplayed()
        verifyNoInteractions(mockCallback)
    }

    @Test
    fun `test that OfflineOptionsBottomSheet does not call onShareOfflineFile for folders when offline`() {
        val mockCallback = mock<() -> Unit>()
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_folder",
            isFolder = true
        )
        setupComposeContent(
            offlineFileInformation = offlineFileInformation,
            isOnline = false,
            onShareOfflineFile = mockCallback
        )

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_SHARE_MENU_ITEM).assertIsNotDisplayed()
        verifyNoInteractions(mockCallback)
    }

    @Test
    fun `test that OfflineOptionsBottomSheet displays file with thumbnail`() {
        val offlineFileInformation = createOfflineFileInformation(
            name = "test_image.jpg",
            isFolder = false,
            thumbnail = "file:///path/to/thumbnail.jpg"
        )
        setupComposeContent(offlineFileInformation)

        composeRule.onNodeWithTag(OFFLINE_OPTIONS_NODE_LIST_VIEW_ITEM).assertIsDisplayed()
        composeRule.onNodeWithText("test_image.jpg").assertIsDisplayed()
    }

    private fun setupComposeContent(
        offlineFileInformation: OfflineFileInformation,
        isOnline: Boolean = false,
        onShareOfflineFile: () -> Unit = {},
        onSaveOfflineFileToDevice: () -> Unit = {},
        onDeleteOfflineFile: () -> Unit = {},
        onOpenOfflineFile: () -> Unit = {},
        onOpenWithFile: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides composeRule.activity) {
                AndroidThemeForPreviews {
                    OfflineOptionsBottomSheetContent(
                        offlineFileInformation = offlineFileInformation,
                        onShareOfflineFile = onShareOfflineFile,
                        onSaveOfflineFileToDevice = onSaveOfflineFileToDevice,
                        onDeleteOfflineFile = onDeleteOfflineFile,
                        onOpenOfflineFile = onOpenOfflineFile,
                        onOpenWithFile = onOpenWithFile,
                        isOnline = isOnline
                    )
                }
            }
        }
    }

    private fun createOfflineFileInformation(
        name: String,
        isFolder: Boolean,
        size: Long = 0L,
        numFiles: Int = 0,
        numFolders: Int = 0,
        thumbnail: String? = null,
    ): OfflineFileInformation {
        val nodeInfo = OtherOfflineNodeInformation(
            id = 1,
            path = "/storage/emulated/0/Mega/offline/$name",
            name = name,
            handle = "123456789",
            isFolder = isFolder,
            lastModifiedTime = 1655292000000,
            parentId = -1
        )

        val folderInfo = if (isFolder) {
            OfflineFolderInfo(
                numFiles = numFiles,
                numFolders = numFolders
            )
        } else null

        return OfflineFileInformation(
            nodeInfo = nodeInfo,
            totalSize = size,
            folderInfo = folderInfo,
            thumbnail = thumbnail
        )
    }
}
