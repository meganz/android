package mega.privacy.android.feature.pdfviewer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.pdfviewer.presentation.components.PDF_PAGE_INDICATOR_TAG
import mega.privacy.android.feature.pdfviewer.presentation.components.PDF_VIEWER_ERROR_DIALOG_TAG
import mega.privacy.android.feature.pdfviewer.presentation.components.PDF_VIEWER_PASSWORD_DIALOG_TAG
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.robolectric.annotation.Config

/**
 * Unit tests for [PdfViewerScreen].
 *
 * Covers BackHandler priority (password → search → back) and top bar title visibility.
 * and password/error dialogs.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class PdfViewerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // Callback mocks
    private val onBack = mock<() -> Unit>()
    private val onMoreClicked = mock<() -> Unit>()
    private val onPageChanged = mock<(Int, Int) -> Unit>()
    private val onLoadComplete = mock<(Int) -> Unit>()
    private val onError = mock<(PdfViewerError) -> Unit>()
    private val onSubmitPassword = mock<(String) -> Unit>()
    private val onDismissPasswordDialog = mock<() -> Unit>()
    private val onDismissErrorDialog = mock<() -> Unit>()
    private val onPasswordInputChanged = mock<() -> Unit>()
    private val onRetry = mock<() -> Unit>()
    private val onUploadToCloudDrive = mock<() -> Unit>()
    private val onActivateSearch = mock<() -> Unit>()
    private val onDeactivateSearch = mock<() -> Unit>()
    private val onSearchQueryChanged = mock<(String) -> Unit>()
    private val onNavigateToNextMatch = mock<() -> Unit>()
    private val onNavigateToPreviousMatch = mock<() -> Unit>()

    @Before
    fun setup() {
        reset(
            onBack,
            onMoreClicked,
            onPageChanged,
            onLoadComplete,
            onError,
            onSubmitPassword,
            onDismissPasswordDialog,
            onDismissErrorDialog,
            onPasswordInputChanged,
            onRetry,
            onUploadToCloudDrive,
            onActivateSearch,
            onDeactivateSearch,
            onSearchQueryChanged,
            onNavigateToNextMatch,
            onNavigateToPreviousMatch,
        )
    }

    private fun setContent(uiState: PdfViewerState = defaultState()) {
        composeTestRule.setContent {
            PdfViewerScreen(
                uiState = uiState,
                onBack = onBack,
                onMoreClicked = onMoreClicked,
                onPageChanged = onPageChanged,
                onLoadComplete = onLoadComplete,
                onError = onError,
                onSubmitPassword = onSubmitPassword,
                onDismissPasswordDialog = onDismissPasswordDialog,
                onDismissErrorDialog = onDismissErrorDialog,
                onPasswordInputChanged = onPasswordInputChanged,
                onRetry = onRetry,
                onUploadToCloudDrive = onUploadToCloudDrive,
                onActivateSearch = onActivateSearch,
                onDeactivateSearch = onDeactivateSearch,
                onSearchQueryChanged = onSearchQueryChanged,
                onNavigateToNextMatch = onNavigateToNextMatch,
                onNavigateToPreviousMatch = onNavigateToPreviousMatch,
            )
        }
    }

    private fun defaultState(
        source: PdfViewerSource? = PdfViewerSource.CloudNode(
            nodeHandle = 12345L,
            contentUri = "content://test.pdf",
            isLocalContent = true,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
        ),
        title: String? = "Test Document.pdf",
        error: PdfViewerError? = null,
        searchState: PdfViewerSearchState = PdfViewerSearchState(),
        currentPage: Int = 1,
        totalPages: Int = 0,
    ) = PdfViewerState(
        isLoading = false,
        source = source,
        title = title,
        error = error,
        searchState = searchState,
        currentPage = currentPage,
        totalPages = totalPages,
    )

    @Test
    fun `test that onDeactivateSearch is called when back is pressed and search is active`() {
        setContent(
            defaultState(
                searchState = PdfViewerSearchState(isSearchActive = true)
            )
        )

        pressBack()

        verify(onDeactivateSearch).invoke()
        verify(onBack, never()).invoke()
        verify(onDismissPasswordDialog, never()).invoke()
    }

    @Test
    fun `test that title is displayed in top bar`() {
        setContent(defaultState(title = "My Document.pdf"))

        composeTestRule.onNodeWithText("My Document.pdf").assertIsDisplayed()
    }

    @Test
    fun `test that regular top bar is displayed when search is not active`() {
        setContent(defaultState(searchState = PdfViewerSearchState(isSearchActive = false)))

        composeTestRule.onNodeWithText("Test Document.pdf").assertIsDisplayed()
    }

    @Test
    fun `test that password dialog is displayed when password error`() {
        setContent(
            defaultState(
                error = PdfViewerError.PasswordProtected,
            )
        )

        composeTestRule
            .onNodeWithTag(PDF_VIEWER_PASSWORD_DIALOG_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that password dialog is displayed when invalid password error`() {
        setContent(
            defaultState(
                error = PdfViewerError.InvalidPassword,
            )
        )
        // InvalidPassword path delays 500ms before showing the dialog to avoid keyboard flicker.
        composeTestRule.mainClock.advanceTimeBy(600)

        composeTestRule
            .onNodeWithTag(PDF_VIEWER_PASSWORD_DIALOG_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that error dialog is displayed when non-password error`() {
        setContent(
            defaultState(
                error = PdfViewerError.FileNotFound,
            )
        )

        composeTestRule
            .onNodeWithTag(PDF_VIEWER_ERROR_DIALOG_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onDismissErrorDialog is invoked when ok button is clicked on error dialog`() {
        setContent(defaultState(error = PdfViewerError.FileNotFound))

        val okLabel = composeTestRule.activity.getString(sharedR.string.general_ok_only)
        composeTestRule.onNodeWithText(okLabel).performClick()

        verify(onDismissErrorDialog).invoke()
    }

    @Test
    fun `test that onDismissPasswordDialog is invoked when cancel button is clicked on password dialog`() {
        setContent(defaultState(error = PdfViewerError.PasswordProtected))

        val cancelLabel =
            composeTestRule.activity.getString(sharedR.string.general_dialog_cancel_button)
        composeTestRule.onNodeWithText(cancelLabel).performClick()

        verify(onDismissPasswordDialog).invoke()
    }

    @Test
    fun `test that page indicator is not displayed when totalPages is 1`() {
        setContent(defaultState(currentPage = 1, totalPages = 1))

        composeTestRule
            .onNodeWithTag(PDF_PAGE_INDICATOR_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that page indicator is not displayed when search is active`() {
        composeTestRule.mainClock.autoAdvance = false
        setContent(
            defaultState(
                currentPage = 2,
                totalPages = 4,
                searchState = PdfViewerSearchState(isSearchActive = true),
            )
        )
        composeTestRule.mainClock.advanceTimeBy(100)

        composeTestRule
            .onNodeWithTag(PDF_PAGE_INDICATOR_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that page indicator is displayed when totalPages is greater than 1`() {
        composeTestRule.mainClock.autoAdvance = false
        setContent(defaultState(currentPage = 2, totalPages = 4))
        composeTestRule.mainClock.advanceTimeBy(100)

        composeTestRule
            .onNodeWithTag(PDF_PAGE_INDICATOR_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onPasswordInputChanged is invoked when user types in password field`() {
        setContent(defaultState(error = PdfViewerError.PasswordProtected))

        composeTestRule
            .onNode(hasSetTextAction())
            .performTextInput("a")

        verify(onPasswordInputChanged).invoke()
    }
}
