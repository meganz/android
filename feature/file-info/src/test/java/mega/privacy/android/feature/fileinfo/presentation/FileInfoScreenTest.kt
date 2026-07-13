package mega.privacy.android.feature.fileinfo.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.TagsNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fileState = FileInfoUiState(
        isLoading = false,
        title = "Presentation.pdf",
        isFile = true,
        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
        fileTypeExtension = "pdf",
        sizeInBytes = 10L * 1024 * 1024,
        creationTime = 1_749_000_000L,
        modificationTime = 1_749_500_000L,
        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        locationFolders = listOf("Documents"),
    )

    private val folderState = FileInfoUiState(
        isLoading = false,
        title = "New folder",
        isFile = false,
        iconRes = iconPackR.drawable.ic_folder_medium_solid,
        fileTypeExtension = null,
        sizeInBytes = 0L,
        creationTime = 1_749_000_000L,
        modificationTime = null,
    )

    @Test
    fun `test that the header, name and type-size subtitle are displayed for a file`() {
        setContent(uiState = fileState)

        composeRule.onNodeWithTag(FILE_INFO_HEADER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Presentation.pdf").assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains("PDF", substring = true)
    }

    @Test
    fun `test that the added and last modified rows are displayed for a file`() {
        setContent(uiState = fileState)

        composeRule.onNodeWithTag(FILE_INFO_ADDED_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_LAST_MODIFIED_TAG).assertExists()
    }

    @Test
    fun `test that the type shows Folder for a folder`() {
        setContent(uiState = folderState)

        composeRule.onNodeWithText("New folder").assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains("Folder", substring = true)
    }

    @Test
    fun `test that a folder shows the added row but not the last modified row`() {
        setContent(uiState = folderState)

        composeRule.onNodeWithTag(FILE_INFO_ADDED_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_LAST_MODIFIED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the loading placeholder is displayed while loading`() {
        setContent(uiState = FileInfoUiState(isLoading = true))

        composeRule.onNodeWithTag(FILE_INFO_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that clicking the location row invokes onLocationClick`() {
        var clicked = false
        setContent(uiState = fileState, onLocationClick = { clicked = true })

        composeRule.onNodeWithTag(FILE_INFO_LOCATION_TAG).performScrollTo().performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that the editable description field shows the current description`() {
        setContent(
            uiState = fileState.copy(
                accessPermission = AccessPermission.OWNER,
                descriptionText = "My description",
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_DESCRIPTION_TAG).assertExists()
        composeRule.onNodeWithText("My description").assertExists()
    }

    @Test
    fun `test that the description field is hidden when not editable and the description is blank`() {
        setContent(
            uiState = fileState.copy(
                accessPermission = AccessPermission.READ,
                descriptionText = "",
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the description is displayed as read-only text when not editable`() {
        setContent(
            uiState = fileState.copy(
                accessPermission = AccessPermission.READ,
                descriptionText = "My description",
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_DESCRIPTION_TAG).assertExists()
        composeRule.onNodeWithText("My description").assertExists()
    }

    @Test
    fun `test that tags are displayed for a node with tags`() {
        setContent(
            uiState = fileState.copy(
                accessPermission = AccessPermission.OWNER,
                tags = listOf("marketing", "2024"),
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_TAGS_TAG).assertExists()
        composeRule.onNodeWithText("#marketing", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("#2024", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that the tags section is hidden when the node is in the rubbish bin`() {
        setContent(
            uiState = fileState.copy(
                accessPermission = AccessPermission.OWNER,
                isNodeInRubbish = true,
                tags = listOf("marketing"),
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that clicking the tags section navigates to TagsNavKey when editable`() {
        var navKey: NavKey? = null
        setContent(
            uiState = fileState.copy(accessPermission = AccessPermission.OWNER),
            onNavigate = { navKey = it },
        )

        composeRule.onNodeWithTag(FILE_INFO_TAGS_TAG).performScrollTo().performClick()

        assertThat(navKey).isEqualTo(TagsNavKey(NODE_HANDLE))
    }

    @Test
    fun `test that the subtitle shows Outgoing share for a shared folder`() {
        setContent(uiState = folderState.copy(sharedContactCount = 3))

        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains("Outgoing share", substring = true)
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains("Folder", substring = true)
    }

    @Test
    fun `test that the shared with row is displayed for a shared folder`() {
        setContent(uiState = folderState.copy(sharedContactCount = 3))

        composeRule.onNodeWithTag(FILE_INFO_SHARED_WITH_TAG).assertExists()
        composeRule.onNodeWithText("3 contacts", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that the shared with row is hidden for a file even with shared contacts`() {
        setContent(uiState = fileState.copy(sharedContactCount = 3))

        composeRule.onNodeWithTag(FILE_INFO_SHARED_WITH_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that clicking the shared with row navigates to FileContactInfoNavKey`() {
        var navKey: NavKey? = null
        setContent(
            uiState = folderState.copy(sharedContactCount = 3),
            onNavigate = { navKey = it },
        )

        composeRule.onNodeWithTag(FILE_INFO_SHARED_WITH_TAG).performScrollTo().performClick()

        assertThat(navKey).isEqualTo(
            FileContactInfoNavKey(folderHandle = NODE_HANDLE, folderName = folderState.title)
        )
    }

    @Test
    fun `test that the owner and permissions rows are displayed for an incoming share`() {
        setContent(
            uiState = folderState.copy(
                ownerName = "John Doe",
                ownerEmail = "johndoe@mail.com",
                accessPermission = AccessPermission.FULL,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_OWNER_TAG).assertExists()
        composeRule.onNodeWithText("John Doe (johndoe@mail.com)", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_PERMISSIONS_TAG).assertExists()
    }

    @Test
    fun `test that the owner row is hidden when not an incoming share`() {
        setContent(uiState = folderState)

        composeRule.onNodeWithTag(FILE_INFO_OWNER_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(FILE_INFO_PERMISSIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that clicking the owner row navigates to ContactInfoNavKey`() {
        var navKey: NavKey? = null
        setContent(
            uiState = folderState.copy(
                ownerName = "John Doe",
                ownerEmail = "johndoe@mail.com",
                accessPermission = AccessPermission.FULL,
            ),
            onNavigate = { navKey = it },
        )

        composeRule.onNodeWithTag(FILE_INFO_OWNER_TAG).performScrollTo().performClick()

        assertThat(navKey).isEqualTo(ContactInfoNavKey("johndoe@mail.com"))
    }

    private fun setContent(
        uiState: FileInfoUiState,
        nodeHandle: Long = NODE_HANDLE,
        onBack: () -> Unit = {},
        onLocationClick: () -> Unit = {},
        onNavigate: (NavKey) -> Unit = {},
        onDescriptionChange: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                FileInfoScreen(
                    uiState = uiState,
                    nodeHandle = nodeHandle,
                    onBack = onBack,
                    onLocationClick = onLocationClick,
                    onNavigate = onNavigate,
                    onDescriptionChange = onDescriptionChange,
                )
            }
        }
    }

    private companion object {
        const val NODE_HANDLE = 123L
    }
}
