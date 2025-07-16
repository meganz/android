package mega.privacy.android.feature.core.snowflakecomponents

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.snowflakecomponents.BottomSheetAction
import mega.privacy.android.core.snowflakecomponents.TEST_TAG_BOTTOM_SHEET_ACTION
import mega.privacy.android.core.snowflakecomponents.TEST_TAG_ICON
import mega.privacy.android.core.snowflakecomponents.TEST_TAG_NAME
import mega.privacy.android.icon.pack.IconPack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class BottomSheetActionTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val fileName = "File name.pdf"
    private val onClick = mock<() -> Unit>()

    @Test
    fun `test that bottom sheet action shows correctly`() {
        initComposeRuleContent()

        with(composeRule) {
            onNodeWithTag(TEST_TAG_BOTTOM_SHEET_ACTION).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ICON, true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_NAME, true).assertIsDisplayed()
            onNodeWithText(fileName).assertIsDisplayed()
        }
    }

    @Test
    fun `test that bottom sheet click action works`() {
        initComposeRuleContent()

        composeRule.onNodeWithTag(TEST_TAG_BOTTOM_SHEET_ACTION).performClick()
        verify(onClick).invoke()
    }

    private fun initComposeRuleContent() =
        composeRule.setContent {
            BottomSheetAction(
                iconPainter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink),
                name = fileName,
                onClick = onClick,
            )
        }
} 