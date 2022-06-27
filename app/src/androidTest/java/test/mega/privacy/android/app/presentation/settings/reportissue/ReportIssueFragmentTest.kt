package test.mega.privacy.android.app.presentation.settings.reportissue

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.fragment.app.FragmentResultListener
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.HiltTestActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.di.settings.ReportIssueUseCases
import mega.privacy.android.app.domain.entity.Progress
import mega.privacy.android.app.domain.usecase.SubmitIssue
import mega.privacy.android.app.presentation.settings.reportissue.ReportIssueFragment
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.*
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.testFragmentTag

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(ReportIssueUseCases::class)
class ReportIssueFragmentTest {

    private val hiltRule = HiltAndroidRule(this)

    private val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule
    var ruleChain: RuleChain = RuleChain.outerRule(hiltRule)
        .around(composeRule)


    @Module
    @InstallIn(SingletonComponent::class)
    object ReportIssueFragmentTestModule {

        val submitIssue = mock<SubmitIssue>()

        @Provides
        fun provideSubmitIssue(): SubmitIssue = submitIssue

    }

    @Before
    fun setUp() {
        runBlocking {
            whenever(ReportIssueFragmentTestModule.submitIssue(any())).thenReturn(
                emptyFlow()
            )
        }
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        Mockito.clearInvocations(
            ReportIssueFragmentTestModule.submitIssue
        )
    }

    @Test
    fun test_that_submit_button_is_disabled_initially() {
        launchFragmentInHiltContainer<ReportIssueFragment>()
        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(not(isEnabled())))
    }

    @Test
    fun test_that_submit_button_is_enabled_when_description_is_entered() {
        launchFragmentInHiltContainer<ReportIssueFragment>()
        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(not(isEnabled())))

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput("A Description")

        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(isEnabled()))
    }

    @Test
    fun test_that_pressing_submit_button_submits_an_issue() {
        launchFragmentInHiltContainer<ReportIssueFragment>()
        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(not(isEnabled())))

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)
        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(isEnabled()))
        onView(withId(R.id.menu_report_issue_submit))
            .perform(ViewActions.click())

        verifyBlocking(ReportIssueFragmentTestModule.submitIssue) { invoke(argThat { description == text }) }
    }

    @Test
    fun test_that_toggling_the_switch_changes_state() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertIsOn()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .performClick()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_attach_logs_label))
            .assertIsOff()
    }

    @Test
    fun test_that_cancelling_an_upload_closes_the_dialog() {
        runBlocking {
            whenever(ReportIssueFragmentTestModule.submitIssue(any())).thenReturn(
                flow {
                    emit(Progress(0.5f))
                    awaitCancellation()
                }
            )
        }

        launchFragmentInHiltContainer<ReportIssueFragment>()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput("A Description")

        onView(withId(R.id.menu_report_issue_submit))
            .perform(ViewActions.click())


        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_uploading_log_file))
            .assertIsDisplayed()

        composeRule.onNodeWithText(fromId(R.string.general_cancel), ignoreCase = true)
            .performClick()


        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_uploading_log_file))
            .assertDoesNotExist()
    }

    @Test
    fun test_that_a_result_is_set_once_completed() {
        val scenario = launchFragmentInHiltContainer<ReportIssueFragment>()
        val listener = mock<FragmentResultListener>()

        scenario?.onActivity {
            it.supportFragmentManager.setFragmentResultListener(testFragmentTag, it, listener)
        }
        onView(withId(R.id.menu_report_issue_submit))
            .check(matches(not(isEnabled())))

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)
        onView(withId(R.id.menu_report_issue_submit))
            .perform(ViewActions.click())

        verify(listener).onFragmentResult(
            any(),
            argThat {
                getString(ReportIssueFragment::class.java.name) == fromId(R.string.settings_help_report_issue_success)
            })
    }

    @Test
    fun test_that_back_press_closes_fragment() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        Espresso.pressBackUnconditionally()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_instructions))
            .assertDoesNotExist()
    }

    @Test
    fun test_that_back_press_displays_confirmation_dialog_if_description_is_present() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_discard_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun test_that_pressing_cancel_on_the_discard_confirmation_dialog_returns_to_the_report_issue_screen() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        composeRule.onNodeWithText(fromId(R.string.general_cancel), ignoreCase = true)
            .performClick()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_discard_dialog_title))
            .assertDoesNotExist()

        composeRule.onNodeWithText(text)
            .assertIsDisplayed()
    }

    @Test
    fun test_that_pressing_discard_on_the_discard_confirmation_dialog_closes_the_fragment() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        composeRule.onNodeWithText(
            fromId(R.string.settings_help_report_issue_discard_button),
            ignoreCase = true
        ).performClick()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_instructions))
            .assertDoesNotExist()
    }

    @Test
    fun test_that_pressing_back_on_the_discard_confirmation_dialog_returns_to_the_report_issue_screen() {
        launchFragmentInHiltContainer<ReportIssueFragment>()

        val text = "A Description"
        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_description_label))
            .performTextInput(text)

        Espresso.closeSoftKeyboard()
        Espresso.pressBack()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_discard_dialog_title))
            .assertIsDisplayed()

        Espresso.pressBack()

        composeRule.onNodeWithText(fromId(R.string.settings_help_report_issue_discard_dialog_title))
            .assertDoesNotExist()

        composeRule.onNodeWithText(text)
            .assertIsDisplayed()
    }

}