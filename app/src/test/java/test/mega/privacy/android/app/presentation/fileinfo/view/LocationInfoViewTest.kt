package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.LocationInfoView
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_LOCATION
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class LocationInfoViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that on click event is fired when location is clicked`() {
        val mock = mock<() -> Unit>()
        composeTestRule.setContent {
            LocationInfoView(location = "Drive", onClick = mock)
        }
        composeTestRule.onNodeWithTag(TEST_TAG_LOCATION).performClick()
        verify(mock).invoke()
    }
}