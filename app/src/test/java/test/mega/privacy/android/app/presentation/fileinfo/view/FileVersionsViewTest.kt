package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.view.FileVersionsView
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_VERSIONS_BUTTON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.onNodeWithPlural

@RunWith(AndroidJUnit4::class)
class FileVersionsViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that versions number text is shown`() {
        val versions = 3
        composeTestRule.setContent {
            FileVersionsView(versions, onClick = { })
        }
        composeTestRule.onNodeWithPlural(R.plurals.number_of_versions, versions).assertExists()
    }

    @Test
    fun `test that on click event is fired when button is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            FileVersionsView(versions = 3, onClick = mock)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_VERSIONS_BUTTON).performClick()
        verify(mock).invoke()
    }
}