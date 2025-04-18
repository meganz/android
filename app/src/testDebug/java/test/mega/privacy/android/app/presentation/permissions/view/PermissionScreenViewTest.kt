package test.mega.privacy.android.app.presentation.permissions.view

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.permissions.view.NewPermissionsScreen
import mega.privacy.android.app.presentation.permissions.view.PermissionAttributes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(qualifiers = "w420dp-h933dp-xxhdpi")
@RunWith(AndroidJUnit4::class)
class PermissionScreenViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that view renders correctly`() {
        composeTestRule.setContent {
            NewPermissionsScreen(
                attributes = PermissionAttributes(
                    title = "Title",
                    description = "Description",
                    bannerText = "Banner Text",
                    image = painterResource(id = R.drawable.ic_apps),
                    primaryButton = "Primary Button" to {},
                    secondaryButton = "Secondary Button" to {}
                ),
                modifier = androidx.compose.ui.Modifier
            )
        }

        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banner Text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Primary Button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Secondary Button").assertIsDisplayed()
    }

    @Test
    fun `test that button clicks executes callback`() {
        val primaryButtonCallback = mock<() -> Unit>()
        val secondaryButtonCallback = mock<() -> Unit>()

        composeTestRule.setContent {
            NewPermissionsScreen(
                attributes = PermissionAttributes(
                    title = "Title",
                    description = "Description",
                    bannerText = "Banner Text",
                    image = painterResource(id = R.drawable.ic_apps),
                    primaryButton = "Primary Button" to primaryButtonCallback,
                    secondaryButton = "Secondary Button" to secondaryButtonCallback
                ),
                modifier = androidx.compose.ui.Modifier
            )
        }

        composeTestRule.onNodeWithText("Primary Button").performClick()
        composeTestRule.onNodeWithText("Secondary Button").performClick()

        // Verify that the callbacks were invoked
        verify(primaryButtonCallback).invoke()
        verify(secondaryButtonCallback).invoke()
    }
}