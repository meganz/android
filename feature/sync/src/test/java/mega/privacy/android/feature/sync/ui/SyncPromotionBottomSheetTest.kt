package mega.privacy.android.feature.sync.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.ui.views.SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG
import mega.privacy.android.feature.sync.ui.views.SyncPromotionBottomSheet
import mega.privacy.android.shared.resources.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class SyncPromotionBottomSheetTest {

    private lateinit var context: Context

    @get:Rule
    var composeRule = createComposeRule()

    private val onUpgrade = mock<() -> Unit>()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun `test that icon is shown`() = runTest {
        initComposeRule()
        composeRule.onNodeWithTag(SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG).assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun `test that what's new label is shown`() = runTest {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_whats_new_label))
            .assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that title is shown`() = runTest {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_title))
            .assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that body is shown`() = runTest {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_body_message))
            .assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that primary button is shown (free account)`() = runTest {
        initComposeRule(isFreeAccount = true)
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_primary_button_text_free))
            .assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that primary button is shown (non free account)`() = runTest {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_primary_button_text_pro))
            .assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that secondary button is shown`() = runTest {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_secondary_button_text))
            .assertExists()
    }

    private fun initComposeRule(
        isFreeAccount: Boolean = false,
    ) {
        composeRule.setContent {
            SyncPromotionBottomSheet(
                upgradeAccountClicked = onUpgrade,
                isFreeAccount = isFreeAccount,
            )
        }
    }
}