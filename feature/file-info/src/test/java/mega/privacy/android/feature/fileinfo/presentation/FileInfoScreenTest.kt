package mega.privacy.android.feature.fileinfo.presentation

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.feature.fileinfo.presentation.view.FILE_INFO_NO_LOCATION_TAG
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.TagsNavKey
import mega.privacy.android.navigation.destination.VersionsFileNavKey
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
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains(
            getString(sharedR.string.file_info_information_type_folder),
            substring = true,
        )
    }

    @Test
    fun `test that the subtitle shows the folder size and file count for a folder`() {
        setContent(
            uiState = folderState.copy(
                sizeInBytes = 21L * 1024 * 1024,
                numberOfFiles = 2,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains("21 MB", substring = true)
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains(
                getQuantityString(sharedR.plurals.num_of_files_with_parameter, 2),
                substring = true,
            )
    }

    @Test
    fun `test that the subtitle shows both folder and file counts for a folder with sub-folders`() {
        setContent(
            uiState = folderState.copy(
                numberOfFolders = 3,
                numberOfFiles = 24,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains(
                getQuantityString(sharedR.plurals.num_of_folders_num_of_files, 3),
                substring = true,
            )
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextContains(
                getQuantityString(sharedR.plurals.num_of_files_with_parameter, 24),
                substring = true,
            )
    }

    @Test
    fun `test that the subtitle has no content count for an empty folder`() {
        setContent(uiState = folderState.copy(numberOfFiles = 0, numberOfFolders = 0))

        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextEquals(getString(sharedR.string.file_info_information_type_folder))
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
    fun `test that the loading skeleton uses the two-pane layout in landscape`() {
        setContent(
            uiState = FileInfoUiState(isLoading = true),
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            deviceType = DeviceType.Tablet,
        )

        composeRule.onNodeWithTag(FILE_INFO_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_DETAILS_TAG).assertExists()
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
    fun `test that the subtitle shows Outgoing share instead of Folder for a shared folder`() {
        setContent(uiState = folderState.copy(sharedContactCount = 3))

        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG)
            .assertTextEquals(getString(sharedR.string.file_info_information_outgoing_share))
    }

    @Test
    fun `test that the shared with row is displayed for a shared folder`() {
        setContent(uiState = folderState.copy(sharedContactCount = 3))

        composeRule.onNodeWithTag(FILE_INFO_SHARED_WITH_TAG).assertExists()
        composeRule.onNodeWithText(
            getQuantityString(sharedR.plurals.file_info_information_num_contacts, 3),
            useUnmergedTree = true,
        ).assertExists()
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
            FileContactInfoNavKey(
                folderHandle = NODE_HANDLE,
                folderName = getString(sharedR.string.file_info_information_shared_with_label),
            )
        )
    }

    @Test
    fun `test that the version rows are displayed for a folder with versioned files`() {
        setContent(
            uiState = folderState.copy(
                numberOfVersions = 91,
                currentVersionsSizeInBytes = 22_800_000_000L,
                previousVersionsSizeInBytes = 1_260_000_000L,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_VERSIONS_TAG).assertExists()
        composeRule.onNodeWithText(
            getQuantityString(sharedR.plurals.file_info_information_num_versioned_files, 91),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_CURRENT_VERSIONS_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_PREVIOUS_VERSIONS_TAG).assertExists()
    }

    @Test
    fun `test that the version rows are hidden for a folder without versions`() {
        setContent(uiState = folderState.copy(numberOfVersions = 0))

        composeRule.onNodeWithTag(FILE_INFO_VERSIONS_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(FILE_INFO_CURRENT_VERSIONS_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(FILE_INFO_PREVIOUS_VERSIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the duration badge and subtitle duration are shown for a video`() {
        setContent(
            uiState = fileState.copy(
                fileTypeExtension = "mov",
                durationText = "1:24",
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_DURATION_BADGE_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains("1:24", substring = true)
    }

    @Test
    fun `test that the duration badge is hidden when there is no duration`() {
        setContent(uiState = fileState.copy(durationText = null))

        composeRule.onNodeWithTag(FILE_INFO_DURATION_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the folder version rows are hidden for a file`() {
        setContent(uiState = fileState.copy(numberOfVersions = 91))

        composeRule.onNodeWithTag(FILE_INFO_CURRENT_VERSIONS_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(FILE_INFO_PREVIOUS_VERSIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the file version row is displayed for a file with versions`() {
        setContent(uiState = fileState.copy(versionCount = 2))

        composeRule.onNodeWithTag(FILE_INFO_VERSIONS_TAG).assertExists()
        composeRule.onNodeWithText(
            getQuantityString(sharedR.plurals.file_info_information_num_versions, 2),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun `test that the file version row is hidden for a file without versions`() {
        setContent(uiState = fileState.copy(versionCount = 0))

        composeRule.onNodeWithTag(FILE_INFO_VERSIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that clicking the file version row navigates to VersionsFileNavKey`() {
        var navKey: NavKey? = null
        setContent(uiState = fileState.copy(versionCount = 2), onNavigate = { navKey = it })

        composeRule.onNodeWithTag(FILE_INFO_VERSIONS_TAG).performScrollTo().performClick()

        assertThat(navKey).isEqualTo(VersionsFileNavKey(NODE_HANDLE))
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

    @Test
    fun `test that the no-location placeholder is shown for an owned media file without coordinates`() {
        setContent(
            uiState = fileState.copy(
                isMediaFile = true,
                accessPermission = AccessPermission.OWNER,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_NO_LOCATION_TAG).assertExists()
    }

    @Test
    fun `test that the no-location placeholder is hidden for a non-media file`() {
        setContent(uiState = fileState.copy(accessPermission = AccessPermission.OWNER))

        composeRule.onNodeWithTag(FILE_INFO_NO_LOCATION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the location section is hidden for a media file the user does not own`() {
        setContent(
            uiState = fileState.copy(
                isMediaFile = true,
                accessPermission = AccessPermission.READ,
            ),
        )

        composeRule.onNodeWithTag(FILE_INFO_NO_LOCATION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the header and details are shown side by side in landscape`() {
        setContent(
            uiState = folderState.copy(
                sizeInBytes = 21L * 1024 * 1024,
                numberOfFiles = 2,
                accessPermission = AccessPermission.OWNER,
                tags = listOf("marketing"),
            ),
            orientation = Configuration.ORIENTATION_LANDSCAPE,
        )

        composeRule.onNodeWithTag(FILE_INFO_HEADER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains(
            getQuantityString(sharedR.plurals.num_of_files_with_parameter, 2),
            substring = true,
        )
        composeRule.onNodeWithTag(FILE_INFO_TAGS_TAG).performScrollTo().assertExists()
    }

    @Test
    fun `test that the header and details split evenly on a phone in landscape`() {
        setContent(
            uiState = folderState,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            deviceType = DeviceType.Phone,
        )

        assertThat(detailsPaneWidth()).isWithin(1f).of(headerWidth())
    }

    @Test
    fun `test that the tablet landscape panes split evenly within a constrained width`() {
        setContent(
            uiState = folderState,
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            deviceType = DeviceType.Tablet,
        )

        val screenWidth = composeRule.onNodeWithTag(FILE_INFO_SCREEN_TAG)
            .getUnclippedBoundsInRoot().let { (it.right - it.left).value }
        val header = composeRule.onNodeWithTag(FILE_INFO_HEADER_TAG).getUnclippedBoundsInRoot()
        val details = composeRule.onNodeWithTag(FILE_INFO_DETAILS_TAG).getUnclippedBoundsInRoot()

        // 50/50 split between header and details.
        assertThat((details.right - details.left).value)
            .isWithin(1f).of((header.right - header.left).value)
        // The two panes plus the row's 16dp side padding span ~70% of the screen, centered.
        val contentWidth = (details.right - header.left).value + 32f
        assertThat(contentWidth / screenWidth).isWithin(0.05f).of(0.7f)
    }

    private fun headerWidth(): Float = composeRule.onNodeWithTag(FILE_INFO_HEADER_TAG)
        .getUnclippedBoundsInRoot().let { (it.right - it.left).value }

    private fun detailsPaneWidth(): Float = composeRule.onNodeWithTag(FILE_INFO_DETAILS_TAG)
        .getUnclippedBoundsInRoot().let { (it.right - it.left).value }

    private fun getString(@StringRes resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources
            .getQuantityString(resId, quantity, quantity)

    private fun setContent(
        uiState: FileInfoUiState,
        nodeHandle: Long = NODE_HANDLE,
        orientation: Int = Configuration.ORIENTATION_PORTRAIT,
        deviceType: DeviceType = DeviceType.Phone,
        onBack: () -> Unit = {},
        onLocationClick: () -> Unit = {},
        onNavigate: (NavKey) -> Unit = {},
        onDescriptionChange: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                this.orientation = orientation
            }
            AndroidThemeForPreviews {
                // Provide inside the theme so these win over the theme's own defaults
                // (the theme sets LocalDeviceType based on the preview window).
                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalDeviceType provides deviceType,
                ) {
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
    }

    private companion object {
        const val NODE_HANDLE = 123L
    }
}
