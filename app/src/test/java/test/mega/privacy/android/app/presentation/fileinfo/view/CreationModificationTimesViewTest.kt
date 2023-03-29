package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.CreationModificationTimesView
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_MODIFICATION_TIME
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreationModificationTimesViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the modification section is shown when modification time is set`() {
        composeTestRule.setContent {
            CreationModificationTimesView(
                creationTimeInSeconds = 123L,
                modificationTimeInSeconds = 456L,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_MODIFICATION_TIME).assertExists()
    }

    @Test
    fun `test that the modification section is not shown when modification time is null`() {
        composeTestRule.setContent {
            CreationModificationTimesView(
                creationTimeInSeconds = 123L,
                modificationTimeInSeconds = null,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_MODIFICATION_TIME).assertDoesNotExist()
    }
}