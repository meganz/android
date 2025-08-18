package mega.privacy.android.app.main.dialog.storagestatus

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.storagestatus.ACHIEVEMENT_TAG
import mega.privacy.android.app.main.dialog.storagestatus.HORIZONTAL_ACTION_TAG
import mega.privacy.android.app.main.dialog.storagestatus.HORIZONTAL_DISMISS_TAG
import mega.privacy.android.app.main.dialog.storagestatus.StorageStatusDialogView
import mega.privacy.android.app.main.dialog.storagestatus.VERTICAL_ACTION_TAG
import mega.privacy.android.app.main.dialog.storagestatus.VERTICAL_DISMISS_TAG
import mega.privacy.android.core.nodecomponents.dialog.storage.StorageStatusDialogState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.fromId

@RunWith(AndroidJUnit4::class)
class StorageStatusDialogViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComponent(
        storageStatusDialogState: StorageStatusDialogState,
        dismissClickListener: () -> Unit = { },
        actionButtonClickListener: () -> Unit = { },
        achievementButtonClickListener: () -> Unit = { },
    ) {
        composeTestRule.setContent {
            StorageStatusDialogView(
                state = storageStatusDialogState,
                dismissClickListener = dismissClickListener,
                actionButtonClickListener = actionButtonClickListener,
                achievementButtonClickListener = achievementButtonClickListener
            )
        }
    }

    private fun getStorageDialogState(
        storageState: StorageState = StorageState.Red,
        accountType: AccountType = AccountType.FREE,
        isAchievementEnabled: Boolean = true,
        overQuotaAlert: Boolean = true,
        preWarning: Boolean = false
    ) = StorageStatusDialogState(
        storageState = storageState,
        accountType = accountType,
        product = null,
        isAchievementsEnabled = isAchievementEnabled,
        overQuotaAlert = overQuotaAlert,
        preWarning = preWarning
    )

    @Test
    fun `test that vertical buttons are shown when isAchievementsEnabled is true`() {
        val storageDialogState = getStorageDialogState()
        setComponent(storageDialogState)
        composeTestRule.onNodeWithTag(VERTICAL_ACTION_TAG).assertExists()
        composeTestRule.onNodeWithTag(VERTICAL_DISMISS_TAG).assertExists()
        composeTestRule.onNodeWithTag(ACHIEVEMENT_TAG).assertExists()
        composeTestRule.onNodeWithTag(HORIZONTAL_ACTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(HORIZONTAL_DISMISS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that horizontal buttons are shown when isAchievementsEnabled is false`() {
        val storageDialogState = getStorageDialogState(isAchievementEnabled = false)
        setComponent(storageDialogState)
        composeTestRule.onNodeWithTag(HORIZONTAL_ACTION_TAG).assertExists()
        composeTestRule.onNodeWithTag(HORIZONTAL_DISMISS_TAG).assertExists()
        composeTestRule.onNodeWithTag(VERTICAL_ACTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(VERTICAL_DISMISS_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ACHIEVEMENT_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that correct title and body text is displayed when overQuotaAlert is false`() {
        val contentText = String.format(fromId(R.string.text_storage_full_warning), "", "")
        val storageDialogState = getStorageDialogState(overQuotaAlert = false)
        setComponent(storageDialogState)
        composeTestRule.onNodeWithText(fromId(id = R.string.action_upgrade_account)).assertExists()
        composeTestRule.onNodeWithText(contentText).assertExists()
    }

    @Test
    fun `test that actionButtonClickListener is fired when horizontal action button is pressed`() {
        val horizontalActionButtonClickListener = mock<() -> Unit>()
        val storageDialogState = getStorageDialogState(isAchievementEnabled = false)
        setComponent(
            storageDialogState,
            actionButtonClickListener = horizontalActionButtonClickListener
        )
        composeTestRule.onNodeWithTag(HORIZONTAL_ACTION_TAG).performClick()
        verify(horizontalActionButtonClickListener).invoke()
    }

    @Test
    fun `test that actionButtonClickListener is fired when vertical action button is pressed`() {
        val verticalActionButtonClickListener = mock<() -> Unit>()
        val storageDialogState = getStorageDialogState(isAchievementEnabled = true)
        setComponent(
            storageDialogState,
            actionButtonClickListener = verticalActionButtonClickListener
        )
        composeTestRule.onNodeWithTag(VERTICAL_ACTION_TAG).performClick()
        verify(verticalActionButtonClickListener).invoke()
    }

    @Test
    fun `test that dismissClickListener is fired when horizontal dismiss button is pressed`() {
        val dismissClickListener = mock<() -> Unit>()
        val storageDialogState = getStorageDialogState(isAchievementEnabled = false)
        setComponent(
            storageDialogState,
            dismissClickListener = dismissClickListener
        )
        composeTestRule.onNodeWithTag(HORIZONTAL_DISMISS_TAG).performClick()
        verify(dismissClickListener).invoke()
    }

    @Test
    fun `test that dismissClickListener is fired when vertical dismiss button is pressed`() {
        val dismissClickListener = mock<() -> Unit>()
        val storageDialogState = getStorageDialogState(isAchievementEnabled = true)
        setComponent(
            storageDialogState,
            dismissClickListener = dismissClickListener
        )
        composeTestRule.onNodeWithTag(VERTICAL_DISMISS_TAG).performClick()
        verify(dismissClickListener).invoke()
    }

    @Test
    fun `test that achievementButtonClickListener is fired when achievement button is pressed`() {
        val achievementButtonClickListener = mock<() -> Unit>()
        val storageDialogState = getStorageDialogState(isAchievementEnabled = true)
        setComponent(
            storageDialogState,
            achievementButtonClickListener = achievementButtonClickListener
        )
        composeTestRule.onNodeWithTag(ACHIEVEMENT_TAG).performClick()
        verify(achievementButtonClickListener).invoke()
    }
}