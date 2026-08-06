package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.CLOUD_EXPLORER_VIEW_TAG
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.explorerViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.transfers.model.UploadFileUiState
import mega.privacy.android.shared.transfers.model.UploadFileViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
internal class ShareFilesToMegaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModelStoreOwner = explorerViewModelStoreOwner()

    private val uploadFileViewModel = mock<UploadFileViewModel> {
        on { uiState } doReturn MutableStateFlow(UploadFileUiState())
    }

    @Test
    fun `test that nothing is rendered immediately while loading`() {
        setContent(uiState = ShareFilesToMegaUiState.Loading)

        composeTestRule.onNodeWithTag(SHARE_FILES_TO_MEGA_PROCESSING_VIEW_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(CLOUD_EXPLORER_VIEW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that processing view is shown when loading outlasts the grace delay`() {
        composeTestRule.mainClock.autoAdvance = false
        setContent(uiState = ShareFilesToMegaUiState.Loading)

        composeTestRule.mainClock.advanceTimeBy(250)

        composeTestRule.onNodeWithTag(SHARE_FILES_TO_MEGA_PROCESSING_VIEW_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CLOUD_EXPLORER_VIEW_TAG).assertDoesNotExist()
    }

    private fun setContent(uiState: ShareFilesToMegaUiState) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    ShareFilesToMegaScreen(
                        uiState = uiState,
                        startNavKey = CopyNavKey(emptyList()),
                        onStartUpload = {},
                        onNavigateBack = {},
                        onNavigate = {},
                        uploadFileViewModel = uploadFileViewModel,
                    )
                }
            }
        }
    }
}
