package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@RunWith(AndroidJUnit4::class)
class ExternalCheckoutInformationBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testScope = TestScope()
    private val onShowInformationNextTimeChanged: (Boolean) -> Unit = mock()
    private val onCancel: () -> Unit = mock()
    private val onContinue: () -> Unit = mock()

    private fun setComposeContent(
        showInformationNextTime: Boolean = true,
        domainUrl: String = "https://mega.nz",
    ) {
        composeTestRule.setContent {
            AndroidTheme(isDark = isSystemInDarkTheme()) {
                ExternalCheckoutInformationBottomSheetContent(
                    showInformationNextTime = showInformationNextTime,
                    onShowInformationNextTimeChanged = onShowInformationNextTimeChanged,
                    onCancel = onCancel,
                    onContinue = onContinue,
                    coroutineScope = testScope,
                    domainUrl = domainUrl,
                )
            }
        }
    }

    @Test
    fun `test that all UI elements are displayed`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_DESCRIPTION)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CHECKBOX)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CHECKBOX_LABEL)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CANCEL)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CONTINUE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that title displays correct text`() {
        setComposeContent()

        val titleText = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(sharedR.string.external_checkout_information_title)
        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
    }

    @Test
    fun `test that description displays correct text`() {
        val domainUrl = "https://mega.nz"
        setComposeContent(domainUrl = domainUrl)

        val descriptionText = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(
                sharedR.string.external_checkout_information_description,
                domainUrl
            )
        composeTestRule.onNodeWithText(descriptionText).assertIsDisplayed()
    }

    @Test
    fun `test that checkbox label displays correct text`() {
        setComposeContent()

        val labelText = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(sharedR.string.external_checkout_show_next_time)
        composeTestRule.onNodeWithText(labelText).assertIsDisplayed()
    }

    @Test
    fun `test that clicking checkbox calls onShowInformationNextTimeChanged`() = runTest {
        setComposeContent(showInformationNextTime = true)

        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CHECKBOX)
            .performClick()

        verify(onShowInformationNextTimeChanged).invoke(false)
        verifyNoMoreInteractions(onShowInformationNextTimeChanged, onCancel, onContinue)
    }

    @Test
    fun `test that clicking cancel button calls onCancel`() = runTest {
        setComposeContent()

        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CANCEL)
            .performClick()

        verify(onCancel).invoke()
        verifyNoMoreInteractions(onShowInformationNextTimeChanged, onCancel, onContinue)
    }

    @Test
    fun `test that clicking continue button calls onContinue`() = runTest {
        setComposeContent()

        composeTestRule.onNodeWithTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CONTINUE)
            .performClick()

        verify(onContinue).invoke()
        verifyNoMoreInteractions(onShowInformationNextTimeChanged, onCancel, onContinue)
    }
}

