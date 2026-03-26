package mega.privacy.android.feature.pdfviewer.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerError
import mega.privacy.android.feature.pdfviewer.presentation.model.PdfViewerSource
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
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class PdfViewerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Callback mocks
    private val onBack = mock<() -> Unit>()
    private val onMoreClicked = mock<() -> Unit>()
    private val onPageChanged = mock<(Int, Int) -> Unit>()
    private val onLoadComplete = mock<(Int) -> Unit>()
    private val onError = mock<(PdfViewerError) -> Unit>()
    private val onSubmitPassword = mock<(String) -> Unit>()
    private val onDismissPasswordDialog = mock<() -> Unit>()
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
        showPasswordDialog: Boolean = false,
        error: PdfViewerError? = null,
        searchState: PdfViewerSearchState = PdfViewerSearchState(),
    ) = PdfViewerState(
        isLoading = false,
        source = source,
        title = title,
        showPasswordDialog = showPasswordDialog,
        error = error,
        searchState = searchState,
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
}
