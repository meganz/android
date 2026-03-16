package mega.privacy.android.navigation.contract.featureflag

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.awaitCancellation
import mega.privacy.android.domain.entity.Feature
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureFlagGateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testFeature = object : Feature {
        override val name = "TestFeature"
        override val description = "A test feature"
    }

    @Test
    fun `test that enabled content is shown when flag is true`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFeatureFlagResolver provides FeatureFlagResolver { true }
            ) {
                FeatureFlagGate(
                    feature = testFeature,
                    disabled = { Text("Disabled") },
                ) {
                    Text("Enabled")
                }
            }
        }

        composeTestRule.onNodeWithText("Enabled").assertIsDisplayed()
    }

    @Test
    fun `test that disabled content is shown when flag is false`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFeatureFlagResolver provides FeatureFlagResolver { false }
            ) {
                FeatureFlagGate(
                    feature = testFeature,
                    disabled = { Text("Disabled") },
                ) {
                    Text("Enabled")
                }
            }
        }

        composeTestRule.onNodeWithText("Disabled").assertIsDisplayed()
    }

    @Test
    fun `test that loading content is shown when flag is pending`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFeatureFlagResolver provides FeatureFlagResolver {
                    awaitCancellation()
                }
            ) {
                FeatureFlagGate(
                    feature = testFeature,
                    loading = { Text("Loading") },
                    disabled = { Text("Disabled") },
                ) {
                    Text("Enabled")
                }
            }
        }

        composeTestRule.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun `test that disabled content is shown when resolver throws an exception`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFeatureFlagResolver provides FeatureFlagResolver {
                    throw RuntimeException("Feature flag error")
                }
            ) {
                FeatureFlagGate(
                    feature = testFeature,
                    disabled = { Text("Disabled") },
                ) {
                    Text("Enabled")
                }
            }
        }

        composeTestRule.onNodeWithText("Disabled").assertIsDisplayed()
    }
}
