package mega.privacy.android.app.presentation.transfers.view.active

import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.model.QuotaWarning
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.TEST_TAG_ACTIVE_TRANSFER_ITEM
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.math.BigInteger

@RunWith(AndroidJUnit4::class)
class ActiveTransfersViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val tag1 = 1
    private val tag2 = 2

    private val state =
        TransferImageUiState(fileTypeResId = iconPackR.drawable.ic_text_medium_solid)

    private val viewModel = mock<ActiveTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ActiveTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn viewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that view is displayed correctly if there are no active transfers`() {
        initComposeTestRule()
        with(composeTestRule) {
            val emptyText =
                activity.getString(sharedR.string.transfers_no_active_transfers_empty_text)
                    .replace("[A]", "").replace("[/A]", "")

            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_EMPTY_VIEW).assertIsDisplayed()
            onNodeWithText(emptyText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that view is displayed correctly if there is one in progress transfer`() {
        val inProgressTransfers = listOf(getTransfer(tag = tag1))

        initComposeTestRule(inProgressTransfers = inProgressTransfers)
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag1").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag2").assertDoesNotExist()
        }
    }

    @Test
    fun `test that view is displayed correctly if there are two in progress transfers`() {
        val inProgressTransfers = listOf(
            getTransfer(tag = tag1),
            getTransfer(tag = tag2),
        )

        initComposeTestRule(inProgressTransfers = inProgressTransfers)
        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFERS_VIEW).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag1").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_ITEM + "_$tag2").assertIsDisplayed()
        }
    }

    @Test
    fun `test that on reorder preview is called when an item is dragged`() {
        val inProgressTransfers = (1..10).map {
            whenever(viewModel.getUiStateFlow(it)) doReturn MutableStateFlow(state)
            getTransfer(tag = it)
        }
        val index = 2
        val transferToDrag = inProgressTransfers[index]
        val dragPositions = 3
        val onReorderPreview = mock<(from: Int, to: Int) -> Unit>()
        val onReorderConfirmed = mock<(InProgressTransfer) -> Unit>()
        initComposeTestRule(
            inProgressTransfers = inProgressTransfers,
            onReorderPreview = onReorderPreview,
            onReorderConfirmed = onReorderConfirmed
        )
        composeTestRule.onNodeWithTag("${TEST_TAG_ACTIVE_TRANSFER_ITEM}_${transferToDrag.tag}")
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(0f, height * dragPositions.toFloat()))
            }

        verify(onReorderPreview).invoke(index, index + dragPositions)
        verifyNoInteractions(onReorderConfirmed)
    }

    @Test
    fun `test that on reorder confirmed is called when a drag is finished`() {
        val inProgressTransfers = (1..10).map {
            whenever(viewModel.getUiStateFlow(it)) doReturn MutableStateFlow(state)
            getTransfer(tag = it)
        }
        val transferToDrag = inProgressTransfers[2]
        val onReorderPreview = mock<(from: Int, to: Int) -> Unit>()
        val onReorderConfirmed = mock<(InProgressTransfer) -> Unit>()
        initComposeTestRule(
            inProgressTransfers = inProgressTransfers,
            onReorderPreview = onReorderPreview,
            onReorderConfirmed = onReorderConfirmed
        )
        composeTestRule.onNodeWithTag("${TEST_TAG_ACTIVE_TRANSFER_ITEM}_${transferToDrag.tag}")
            .performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                moveBy(Offset(0f, height * 2f))
                up()
            }

        verify(onReorderPreview).invoke(any(), any())
        verify(onReorderConfirmed).invoke(transferToDrag)
    }

    @Test
    fun `test that over quota banner is displayed when quotaWarning is StorageAndTransfer`() =
        runTest {
            initComposeTestRule(
                inProgressTransfers = listOf(getTransfer(1)),
                quotaWarning = QuotaWarning.StorageAndTransfer,
            )

            composeTestRule.onNodeWithTag(OVER_QUOTA_BANNER_TAG).assertIsDisplayed()
        }

    @Test
    fun `test that over quota banner is displayed when quotaWarning is Transfer`() =
        runTest {
            initComposeTestRule(
                inProgressTransfers = listOf(getTransfer(1)),
                quotaWarning = QuotaWarning.Transfer,
            )

            composeTestRule.onNodeWithTag(OVER_QUOTA_BANNER_TAG).assertIsDisplayed()
        }

    @Test
    fun `test that over quota banner is displayed when quotaWarning is Storage`() =
        runTest {
            initComposeTestRule(
                inProgressTransfers = listOf(getTransfer(1)),
                quotaWarning = QuotaWarning.Storage,
            )

            composeTestRule.onNodeWithTag(OVER_QUOTA_BANNER_TAG).assertIsDisplayed()
        }

    @Test
    fun `test that over quota banner is not displayed when quotaWarning is null`() = runTest {
        initComposeTestRule(
            inProgressTransfers = listOf(getTransfer(1)),
            quotaWarning = null,
        )

        composeTestRule.onNodeWithTag(OVER_QUOTA_BANNER_TAG).assertDoesNotExist()
    }

    private fun initComposeTestRule(
        inProgressTransfers: List<InProgressTransfer> = emptyList(),
        isOverQuota: Boolean = false,
        quotaWarning: QuotaWarning? = null,
        areTransfersPaused: Boolean = false,
        onReorderPreview: (from: Int, to: Int) -> Unit = { _, _ -> },
        onReorderConfirmed: (InProgressTransfer) -> Unit = {},
        onUpgradeClick: () -> Unit = {},
        onConsumeQuotaWarning: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                ActiveTransfersView(
                    activeTransfers = inProgressTransfers.toImmutableList(),
                    isTransferOverQuota = isOverQuota,
                    isStorageOverQuota = isOverQuota,
                    quotaWarning = quotaWarning,
                    areTransfersPaused = areTransfersPaused,
                    onPlayPauseClicked = { },
                    onReorderPreview = onReorderPreview,
                    onReorderConfirmed = onReorderConfirmed,
                    selectedActiveTransfersIds = null,
                    onActiveTransferSelected = {},
                    lazyListState = rememberLazyListState(),
                    onUpgradeClick = onUpgradeClick,
                    onConsumeQuotaWarning = onConsumeQuotaWarning,
                )
            }
        }
    }

    private fun getTransfer(tag: Int) = InProgressTransfer.Download(
        uniqueId = tag.toLong(),
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