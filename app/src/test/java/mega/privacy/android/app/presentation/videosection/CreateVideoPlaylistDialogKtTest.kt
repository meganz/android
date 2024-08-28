package mega.privacy.android.app.presentation.videosection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants
import mega.privacy.android.app.presentation.videosection.view.playlist.CreateVideoPlaylistDialog
import mega.privacy.android.app.presentation.videosection.view.playlist.ERROR_MESSAGE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.POSITIVE_BUTTON_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class CreateVideoPlaylistDialogKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    private fun setComposeContent(
        title: String = "",
        positiveButtonText: String = "",
        onDialogPositiveButtonClicked: (title: String) -> Unit = { },
        onDismissRequest: () -> Unit = {},
        onDialogInputChange: (Boolean) -> Unit = {},
        initialInputText: () -> String = { "" },
        inputPlaceHolderText: () -> String = { "" },
        errorMessage: Int? = null,
        isInputValid: () -> Boolean = { true },
    ) {
        composeTestRule.setContent {
            CreateVideoPlaylistDialog(
                title = title,
                positiveButtonText = positiveButtonText,
                onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
                onDismissRequest = onDismissRequest,
                onDialogInputChange = onDialogInputChange,
                initialInputText = initialInputText,
                inputPlaceHolderText = inputPlaceHolderText,
                errorMessage = errorMessage,
                isInputValid = isInputValid
            )
        }
    }

    @Test
    fun `test that the error message is displayed correctly when errorMessage is invalid_string`() {
        setComposeContent(
            errorMessage = R.string.invalid_string,
            isInputValid = { false }
        )

        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG)
            .assertTextEquals(fromId(R.string.invalid_string))
    }

    @Test
    fun `test that the error message is displayed correctly when errorMessage is invalid_characters_defined`() {
        setComposeContent(
            errorMessage = R.string.invalid_characters_defined,
            isInputValid = { false }
        )

        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertTextEquals(
            fromId(id = R.string.invalid_characters_defined).replace(
                "%1\$s",
                StringsConstants.INVALID_CHARACTERS
            )
        )
    }

    @Test
    fun `test that the error message is displayed correctly when errorMessage is others`() {
        setComposeContent(
            errorMessage = 1,
            isInputValid = { false }
        )

        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertTextEquals(
            "A playlist with this name already exists. Enter a different name."
        )
    }

    @Test
    fun `test that the error message is not displayed when errorMessage is null`() {
        setComposeContent(
            errorMessage = null,
            isInputValid = { false }
        )

        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that the error message is not displayed when isInputValid is true`() {
        setComposeContent(
            errorMessage = R.string.invalid_string,
            isInputValid = { true }
        )

        composeTestRule.onNodeWithTag(ERROR_MESSAGE_TEST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that clicking negative button on dialog calls the correct function`() {
        val onDismissRequest = mock<() -> Unit>()
        setComposeContent(
            onDismissRequest = onDismissRequest,
        )

        composeTestRule.onNodeWithText(R.string.general_cancel).performClick()

        verify(onDismissRequest).invoke()
    }

    @Test
    fun `test that the positive dialog button calls the correct function`() {
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()

        setComposeContent(
            onDialogPositiveButtonClicked = onDialogPositiveButtonClicked
        )

        composeTestRule.onNodeWithTag(POSITIVE_BUTTON_TEST_TAG).performClick()

        verify(onDialogPositiveButtonClicked).invoke("")
    }

    @Test
    fun `test that with input clicking the positive dialog button calls the correct function`() {
        val expectedTitle = "New playlist"
        val inputPlaceholderText = R.string.invalid_string
        val onDialogPositiveButtonClicked = mock<(String) -> Unit>()

        setComposeContent(
            onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
            inputPlaceHolderText = { fromId(inputPlaceholderText) }
        )

        composeTestRule.onNodeWithText(inputPlaceholderText)
            .performTextInput(expectedTitle)
        composeTestRule.onNodeWithTag(POSITIVE_BUTTON_TEST_TAG).performClick()

        verify(onDialogPositiveButtonClicked).invoke(expectedTitle)
    }
}