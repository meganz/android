package test.mega.privacy.android.app.main.dialog.storagestatus

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.main.dialog.storagestatus.ACHIEVEMENT_TAG
import mega.privacy.android.app.main.dialog.storagestatus.HORIZONTAL_ACTION_TAG
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusUiState
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusViewModel
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class StorageStatusDialogViewHelperTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that on upgrade and on close are invoked when upgrade button is clicked`() {
        val onClose = mock<() -> Unit>()
        val onUpgradeClick = mock<() -> Unit>()
        setContent(
            StorageStatusUiState(),
            onUpgradeClick = onUpgradeClick,
            onClose = onClose,
        )
        composeTestRule.onNodeWithTag(HORIZONTAL_ACTION_TAG).performClick()
        verify(onUpgradeClick).invoke()
        verify(onClose).invoke()
    }

    @Test
    fun `test that on customized plan and on close are invoked when customized plan button is clicked`() {
        val onClose = mock<() -> Unit>()
        val onCustomizedPlanClick: (email: String, accountType: AccountType) -> Unit = mock()
        setContent(
            StorageStatusUiState(accountType = AccountType.PRO_III),
            onCustomizedPlanClick = onCustomizedPlanClick,
            onClose = onClose,
        )
        composeTestRule.onNodeWithTag(HORIZONTAL_ACTION_TAG).performClick()
        verify(onCustomizedPlanClick).invoke(anyOrNull(), any())
        verify(onClose).invoke()
    }

    @Test
    fun `test that on achievements and on close are invoked when achievements button is clicked`() {
        val onClose = mock<() -> Unit>()
        val onAchievementsClick = mock<() -> Unit>()
        setContent(
            StorageStatusUiState(isAchievementsEnabled = true),
            onAchievementsClick = onAchievementsClick,
            onClose = onClose,
        )
        composeTestRule.onNodeWithTag(ACHIEVEMENT_TAG).performClick()
        verify(onAchievementsClick).invoke()
        verify(onClose).invoke()
    }

    private fun setContent(
        state: StorageStatusUiState,
        onClose: () -> Unit = mock(),
        onUpgradeClick: () -> Unit = mock(),
        onCustomizedPlanClick: (email: String, accountType: AccountType) -> Unit = mock(),
        onAchievementsClick: () -> Unit = mock(),
    ) {
        val viewModel = Mockito.mock<StorageStatusViewModel>()
        whenever(viewModel.state).thenReturn(MutableStateFlow(state))
        composeTestRule.setContent {
            StorageStatusDialogView(
                viewModel = viewModel,
                storageState = StorageState.Orange,
                preWarning = true,
                overQuotaAlert = true,
                onUpgradeClick = onUpgradeClick,
                onCustomizedPlanClick = onCustomizedPlanClick,
                onAchievementsClick = onAchievementsClick,
                onClose = onClose,
            )
        }
    }
}