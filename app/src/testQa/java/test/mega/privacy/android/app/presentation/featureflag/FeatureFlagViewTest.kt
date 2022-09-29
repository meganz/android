package test.mega.privacy.android.app.presentation.featureflag

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.featureflag.FEATURE_FLAG_LIST_TAG
import mega.privacy.android.app.presentation.featureflag.FeatureFlagList
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureFlagViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test list screen if feature list is empty`() {
        setComposeRule(emptyList())
        composeRule.onNodeWithTag(FEATURE_FLAG_LIST_TAG).onChildren().assertCountEquals(0)
    }

    @Test
    fun `test list screen if feature list is not empty`() {
        setComposeRule(getFeatureFlagList())
        composeRule.onNodeWithTag(FEATURE_FLAG_LIST_TAG).onChildren()
            .assertCountEquals(4)
    }

    @Test
    fun `test first and last list element from feature flag list`() {
        setComposeRule(getFeatureFlagList())
        composeRule.apply {
            onNodeWithTag(FEATURE_FLAG_LIST_TAG)
                .onChildren()
                .onFirst()
                .assert(hasText("A feature"))

            onNodeWithTag(FEATURE_FLAG_LIST_TAG)
                .onChildren()
                .onLast()
                .assert(hasText("D feature"))
        }
    }

    @Test
    fun `test if ui element is toggleable`() {
        setComposeRule(getFeatureFlagList())
        composeRule.onNodeWithTag(FEATURE_FLAG_LIST_TAG)
            .onChildren()
            .onLast().assertIsToggleable()
    }

    private fun setComposeRule(list: List<FeatureFlag>) {
        composeRule.setContent {
            FeatureFlagList(featureFlagList = list,
                onCheckedChange = { _, _ -> },
                modifier = Modifier)
        }
    }

    private fun getFeatureFlagList(): List<FeatureFlag> = listOf(
        FeatureFlag("A feature", "", true),
        FeatureFlag("B feature", "", false),
        FeatureFlag("C feature", "", true),
        FeatureFlag("D feature", "", false),
    )
}