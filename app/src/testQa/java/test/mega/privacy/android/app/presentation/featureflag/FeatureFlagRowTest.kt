package test.mega.privacy.android.app.presentation.featureflag

import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.featureflag.FeatureFlagRow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureFlagRowTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test if ui element is toggleable`() {
        composeRule.setContent {
            FeatureFlagRow(name = "name",
                description = "description",
                isEnabled = false,
                onCheckedChange = { _, _ -> })
        }
        composeRule.onRoot().onChild().assertIsToggleable()
    }
}