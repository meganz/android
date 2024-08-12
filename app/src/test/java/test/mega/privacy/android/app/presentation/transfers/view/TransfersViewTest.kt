package test.mega.privacy.android.app.presentation.transfers.view

import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_MORE_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_PAUSE_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransferMenuAction.Companion.TEST_TAG_RESUME_ACTION
import mega.privacy.android.app.presentation.transfers.model.TransfersUiState
import mega.privacy.android.app.presentation.transfers.model.image.InProgressTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.app.presentation.transfers.view.TransfersView
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class TransfersViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onPlayPauseTransfer: (Int) -> Unit = mock()
    private val onResumeTransfers: () -> Unit = mock()
    private val onPauseTransfers: () -> Unit = mock()
    private val showInProgressModal: () -> Unit = mock()
    private val tag1 = 1
    private val tag2 = 2
    private val state = TransferImageUiState(fileTypeResId = R.drawable.ic_text_medium_solid)
    private val viewModel = mock<InProgressTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(InProgressTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn viewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }
    private val inProgressTransfers = listOf(
        getTransfer(tag = tag1),
        getTransfer(tag = tag2),
    ).toImmutableList()

    @Test
    fun `test that pause TransferMenuAction is displayed if transfers are not already paused and click action invokes correctly`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                inProgressTransfers = inProgressTransfers,
                areTransfersPaused = false
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_PAUSE_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onPauseTransfers).invoke()
            }
        }
    }

    @Test
    fun `test that resume TransferMenuAction is displayed if transfers are already paused and click action invokes correctly`() {
        initComposeTestRule(
            uiState = TransfersUiState(
                inProgressTransfers = inProgressTransfers,
                areTransfersPaused = true,
            )
        )
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_RESUME_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(onResumeTransfers).invoke()
            }
        }
    }

    @Test
    fun `test that cancel all transfers TransferMenuAction is displayed if transfers are already paused and click action invokes correctly`() {
        initComposeTestRule(uiState = TransfersUiState(inProgressTransfers = inProgressTransfers))
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_MORE_ACTION).apply {
                assertIsDisplayed()
                performClick()
                verify(showInProgressModal).invoke()
            }
        }
    }

    @OptIn(ExperimentalMaterialNavigationApi::class)
    private fun initComposeTestRule(uiState: TransfersUiState) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                TransfersView(
                    bottomSheetNavigator = rememberBottomSheetNavigator(),
                    scaffoldState = rememberScaffoldState(),
                    onBackPress = {},
                    uiState = uiState,
                    onTabSelected = {},
                    onPlayPauseTransfer = onPlayPauseTransfer,
                    onResumeTransfers = onResumeTransfers,
                    onPauseTransfers = onPauseTransfers,
                    onMoreInProgressActions = showInProgressModal,
                )
            }
        }
    }

    private fun getTransfer(tag: Int) = InProgressTransfer.Download(
        tag = tag,
        totalBytes = 100,
        isPaused = false,
        fileName = "name",
        speed = 100,
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ONE,
        progress = Progress(0.5F),
        nodeId = NodeId(1),
    )
}