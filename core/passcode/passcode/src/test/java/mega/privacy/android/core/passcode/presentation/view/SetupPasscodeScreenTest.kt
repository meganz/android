package mega.privacy.android.core.passcode.presentation.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.core.passcode.presentation.model.PasscodeEntryStage
import mega.privacy.android.core.passcode.presentation.model.PasscodeTypeOption
import mega.privacy.android.core.passcode.presentation.model.PasscodeUIType
import mega.privacy.android.core.passcode.presentation.model.SetupPasscodeUiState
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.PasscodeSettingScreenEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupPasscodeScreenTest {

    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    @Test
    fun `test that the set passcode titles are shown on the first entry`() {
        setContent(stage = PasscodeEntryStage.FirstEntry, isEdit = false)

        composeRule.onNodeWithText(string(R.string.settings_passcode_lock)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.unlock_pin_title)).assertIsDisplayed()
    }

    @Test
    fun `test that the set passcode titles are shown on the confirmation round`() {
        setContent(stage = confirmation(), isEdit = false)

        composeRule.onNodeWithText(string(R.string.unlock_pin_title_2)).assertIsDisplayed()
    }

    @Test
    fun `test that the change passcode titles are shown on the first entry`() {
        setContent(stage = PasscodeEntryStage.FirstEntry, isEdit = true)

        composeRule.onNodeWithText(string(R.string.title_change_passcode)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.reset_pin_title)).assertIsDisplayed()
    }

    @Test
    fun `test that the change passcode titles are shown on the confirmation round`() {
        setContent(stage = confirmation(), isEdit = true)

        composeRule.onNodeWithText(string(R.string.reset_pin_title_2)).assertIsDisplayed()
    }

    @Test
    fun `test that the passcode options button is shown on the first entry`() {
        setContent(stage = PasscodeEntryStage.FirstEntry)

        composeRule.onNodeWithTag(SETUP_PASSCODE_OPTIONS_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the passcode options button is hidden on the confirmation round`() {
        setContent(stage = confirmation())

        composeRule.onNodeWithTag(SETUP_PASSCODE_OPTIONS_BUTTON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the mismatch error is hidden when the confirmation has not failed`() {
        setContent(stage = confirmation(showMismatchError = false))

        composeRule.onNodeWithTag(SETUP_PASSCODE_MISMATCH_ERROR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the mismatch error is shown when the confirmation has failed`() {
        setContent(stage = confirmation(showMismatchError = true))

        composeRule.onNodeWithTag(SETUP_PASSCODE_MISMATCH_ERROR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that a password field is shown for an alphanumeric passcode`() {
        setContent(passcodeType = PasscodeUIType.Alphanumeric(false))

        composeRule.onNodeWithTag(SETUP_PASSCODE_PASSWORD_FIELD_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onPasscodeEntered is called once the pin is complete`() {
        val entered = mutableListOf<String>()
        setContent(onPasscodeEntered = { entered.add(it) })

        passcodeInput().performTextInput("1234")

        assertThat(entered).containsExactly("1234")
    }

    @Test
    fun `test that onPasscodeTypeSelected is called when an option is chosen`() {
        val selected = mutableListOf<PasscodeTypeOption>()
        setContent(onPasscodeTypeSelected = { selected.add(it) })

        composeRule.onNodeWithTag(SETUP_PASSCODE_OPTIONS_BUTTON_TAG).performClick()
        composeRule.onNodeWithText(string(R.string.six_pin_lock)).performClick()

        assertThat(selected).containsExactly(PasscodeTypeOption.SixDigits)
    }

    @Test
    fun `test that setText forwards a passcode of the expected length`() {
        val entered = mutableListOf<String>()
        setContent(
            passcodeType = PasscodeUIType.Pin(false, 4),
            onPasscodeEntered = { entered.add(it) },
        )

        composeRule.onNodeWithTag(SETUP_PASSCODE_FIELD_TAG)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("1234")) }

        assertThat(entered).containsExactly("1234")
    }

    @Test
    fun `test that setText rejects a passcode shorter than the expected length`() {
        val entered = mutableListOf<String>()
        setContent(
            passcodeType = PasscodeUIType.Pin(false, 4),
            onPasscodeEntered = { entered.add(it) },
        )

        composeRule.onNodeWithTag(SETUP_PASSCODE_FIELD_TAG)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("12")) }

        assertThat(entered).isEmpty()
    }

    @Test
    fun `test that setText rejects a passcode longer than the expected length`() {
        val entered = mutableListOf<String>()
        setContent(
            passcodeType = PasscodeUIType.Pin(false, 6),
            onPasscodeEntered = { entered.add(it) },
        )

        composeRule.onNodeWithTag(SETUP_PASSCODE_FIELD_TAG)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("12345678")) }

        assertThat(entered).isEmpty()
    }

    @Test
    fun `test that the passcode type chooser is not reopened after process death`() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = PasscodeUIType.Pin(false, 4),
                    stage = PasscodeEntryStage.FirstEntry,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = false,
                onPasscodeEntered = {},
                onPasscodeTypeSelected = {},
                onNavigateBack = {},
            )
        }
        composeRule.onNodeWithTag(SETUP_PASSCODE_OPTIONS_BUTTON_TAG).performClick()
        composeRule.onNodeWithText(string(R.string.six_pin_lock)).assertIsDisplayed()

        restorationTester.emulateSavedInstanceStateRestore()

        // The entry round and the selected type live in the ViewModel, which does not survive
        // process death either, so the sheet must not reopen over a screen that has reset.
        composeRule.onNodeWithText(string(R.string.six_pin_lock)).assertDoesNotExist()
    }

    @Test
    fun `test that the screen view event is tracked`() {
        setContent()

        assertThat(analyticsRule.events).contains(PasscodeSettingScreenEvent)
    }

    @Test
    fun `test that the screen view event is tracked once when the state changes`() {
        val stage = mutableStateOf<PasscodeEntryStage>(PasscodeEntryStage.FirstEntry)
        composeRule.setContent {
            val currentStage by stage
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = PasscodeUIType.Pin(false, 4),
                    stage = currentStage,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = false,
                onPasscodeEntered = {},
                onPasscodeTypeSelected = {},
                onNavigateBack = {},
            )
        }

        stage.value = confirmation()
        composeRule.waitForIdle()

        assertThat(analyticsRule.events.count { it == PasscodeSettingScreenEvent }).isEqualTo(1)
    }

    @Test
    fun `test that a partially typed passcode is discarded when the passcode type changes`() {
        val entered = mutableListOf<String>()
        val passcodeType = mutableStateOf<PasscodeUIType>(PasscodeUIType.Pin(false, 4))
        composeRule.setContent {
            val type by passcodeType
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = type,
                    stage = PasscodeEntryStage.FirstEntry,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = false,
                onPasscodeEntered = { entered.add(it) },
                onPasscodeTypeSelected = {},
                onNavigateBack = {},
            )
        }

        passcodeInput().performTextInput("12")
        passcodeType.value = PasscodeUIType.Pin(false, 6)
        passcodeInput().performTextInput("123456")

        assertThat(entered).containsExactly("123456")
    }

    @Test
    fun `test that a partially typed passcode is discarded when the entry round changes`() {
        val entered = mutableListOf<String>()
        val stage = mutableStateOf<PasscodeEntryStage>(PasscodeEntryStage.FirstEntry)
        composeRule.setContent {
            val currentStage by stage
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = PasscodeUIType.Pin(false, 4),
                    stage = currentStage,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = false,
                onPasscodeEntered = { entered.add(it) },
                onPasscodeTypeSelected = {},
                onNavigateBack = {},
            )
        }

        passcodeInput().performTextInput("12")
        stage.value = confirmation()
        passcodeInput().performTextInput("3456")

        assertThat(entered).containsExactly("3456")
    }

    /**
     * The screen's own tag sits on the cell row, whose merged semantics hide the focus action that
     * text input needs, so typing has to target the text field wrapping it.
     */
    private fun passcodeInput() = composeRule.onNode(
        hasSetTextAction() and hasAnyDescendant(hasTestTag(SETUP_PASSCODE_FIELD_TAG)),
        useUnmergedTree = true,
    )

    private fun confirmation(showMismatchError: Boolean = false) =
        PasscodeEntryStage.Confirmation(firstEntry = "1234", showMismatchError = showMismatchError)

    private fun string(resId: Int) = composeRule.activity.getString(resId)

    private fun setContent(
        passcodeType: PasscodeUIType = PasscodeUIType.Pin(false, 4),
        stage: PasscodeEntryStage = PasscodeEntryStage.FirstEntry,
        isEdit: Boolean = false,
        onPasscodeEntered: (String) -> Unit = {},
        onPasscodeTypeSelected: (PasscodeTypeOption) -> Unit = {},
    ) {
        composeRule.setContent {
            SetupPasscodeScreen(
                uiState = SetupPasscodeUiState.Data(
                    passcodeType = passcodeType,
                    stage = stage,
                    passcodeSavedEvent = consumed,
                ),
                isEdit = isEdit,
                onPasscodeEntered = onPasscodeEntered,
                onPasscodeTypeSelected = onPasscodeTypeSelected,
                onNavigateBack = {},
            )
        }
    }
}
