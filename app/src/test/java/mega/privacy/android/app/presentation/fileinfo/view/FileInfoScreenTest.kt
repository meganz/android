package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.fromId
import mega.privacy.android.app.fromPluralId
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that preview is shown if preview uri is set`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(previewUriString = "something", hasPreview = true)
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW, true).assertExists()
    }

    @Test
    fun `test that icon is not shown if icon resource is set but has preview`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                previewUriString = "something",
                hasPreview = true,
                iconResource = android.R.drawable.ic_menu_more
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON, true).assertDoesNotExist()
    }

    @Test
    fun `test that undecrypted file shows undecrypted file title`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecretFile.txt",
                isFile = true,
                isDecrypted = false
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        ).assert(hasAnyChild(hasText(fromPluralId(R.plurals.cloud_drive_undecrypted_file, 1))))

    }

    @Test
    fun `test that undecrypted folder shows undecrypted folder title`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecretFolder",
                isFile = false,
                isDecrypted = false
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        )
            .assert(hasAnyChild(hasText(fromId(R.string.shared_items_verify_credentials_undecrypted_folder))))
    }

    @Test
    fun `test that decrypted file shows original title`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecretFile1.txt",
                isFile = true,
                isDecrypted = true
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        )
            .assert(hasAnyChild(hasText("MySecretFile1.txt")))
    }

    @Test
    fun `test that default viewstate shows original title for file`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecretFile2.txt",
                isFile = true,
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }

        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        )
            .assert(hasAnyChild(hasText("MySecretFile2.txt")))
    }

    @Test
    fun `test that decrypted folder shows original title`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecret folder1",
                isFile = false,
                isDecrypted = true
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }
        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        )
            .assert(hasAnyChild(hasText("MySecret folder1")))
    }

    @Test
    fun `test that default viewstate shows original title for folder`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                title = "MySecret folder2",
                isFile = false,
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSetDescriptionClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                onVerifyContactClick = {},
                onAddTagClick = {},
                getAddress = { _, _, _ -> null },
                onShareContactOptionsDismissed = {},
                onSharedWithContactRemoveClicked = {},
                onSharedWithContactMoreInfoClick = {},
                onSharedWithContactChangePermissionClicked = {},
            )
        }

        composeTestRule.onNode(
            hasTestTag(TEST_TAG_FILE_INFO_HEADER)
        )
            .assert(hasAnyChild(hasText("MySecret folder2")))
    }

}