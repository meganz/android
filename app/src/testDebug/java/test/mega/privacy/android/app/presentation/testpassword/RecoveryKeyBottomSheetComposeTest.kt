package test.mega.privacy.android.app.presentation.testpassword

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_COPY
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_PRINT
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_SAVE
import mega.privacy.android.app.presentation.testpassword.view.Constants.BOTTOM_SHEET_TITLE
import mega.privacy.android.app.presentation.testpassword.view.RecoveryKeyBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
@OptIn(ExperimentalMaterialApi::class)
class RecoveryKeyBottomSheetComposeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent() {
        composeTestRule.setContent {
            RecoveryKeyBottomSheet(
                modalSheetState = ModalBottomSheetState(ModalBottomSheetValue.Expanded),
                onPrint = {},
                onCopy = {},
                onSave = {}
            )
        }
    }

    @Test
    fun `test that recovery key bottom sheet should render with correct content`() {
        setComposeContent()

        assertAllView { assertIsDisplayed() }
    }

    @Test
    fun `test that recovery key bottom sheet should be hidden when print is selected`() {
        assertHideWhenMenuClicked(BOTTOM_SHEET_PRINT)
    }

    @Test
    fun `test that recovery key bottom sheet should be hidden when copy is selected`() {
        assertHideWhenMenuClicked(BOTTOM_SHEET_COPY)
    }

    @Test
    fun `test that recovery key bottom sheet should be hidden when save is selected`() {
        assertHideWhenMenuClicked(BOTTOM_SHEET_SAVE)
    }

    private fun assertAllView(assertion: SemanticsNodeInteraction.() -> Unit) {
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_TITLE).let(assertion)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_PRINT).let(assertion)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_COPY).let(assertion)
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_SAVE).let(assertion)
    }

    private fun assertHideWhenMenuClicked(tag: String) {
        setComposeContent()

        composeTestRule.onNodeWithTag(tag).performClick()

        assertAllView { assertIsNotDisplayed() }
    }
}