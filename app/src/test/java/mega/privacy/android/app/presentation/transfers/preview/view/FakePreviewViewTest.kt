package mega.privacy.android.app.presentation.transfers.preview.view

import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.transfers.preview.model.FakePreviewState
import mega.privacy.android.app.presentation.transfers.starttransfer.StartTransfersComponentViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class TransfersViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val startTransfersComponentViewModel = mock<StartTransfersComponentViewModel> {
        on { uiState } doReturn MutableStateFlow(StartTransferViewState())
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(StartTransfersComponentViewModel::class.java.canonicalName.orEmpty()) }) } doReturn startTransfersComponentViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that the view is displayed correctly`() {
        val fileName = "test.txt"
        val uiState = FakePreviewState(
            fileName = fileName,
            fileTypeResId = iconPackR.drawable.ic_text_medium_solid,
        )

        initComposeTestRule(uiState)

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_FAKE_PREVIEW).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PROGRESS_BAR).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_FILE_TYPE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_LOADING_TEST).assertIsDisplayed()
            onNodeWithText(sharedR.string.transfers_fake_preview_text).isDisplayed()
        }
    }

    @OptIn(ExperimentalMaterialNavigationApi::class)
    private fun initComposeTestRule(uiState: FakePreviewState) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                FakePreviewView(
                    scaffoldState = rememberScaffoldState(),
                    onBackPress = {},
                    uiState = uiState,
                    consumeTransferEvent = {},
                    navigateToStorageSettings = {},
                )
            }
        }
    }
}