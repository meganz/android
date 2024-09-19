package mega.privacy.android.feature.sync.ui

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.feature.sync.ui.views.SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG
import mega.privacy.android.feature.sync.ui.views.SyncPromotionBottomSheet
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.resources.R
import mega.privacy.mobile.analytics.event.SyncPromotionBottomSheetLearnMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncPromotionBottomSheetSyncFoldersButtonPressedEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class SyncPromotionBottomSheetTest {

    private lateinit var context: Context
    private lateinit var modalSheetState: ModalBottomSheetState
    private lateinit var coroutineScope: CoroutineScope

    private val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

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
        coroutineScope.launch {
            modalSheetState.show()

            composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_secondary_button_text))
                .assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun `test that click the primary button on a non free account sends the right analytics tracker event`() {
        initComposeRule()
        composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_primary_button_text_pro))
            .assertExists().assertIsDisplayed().performClick()
        assertThat(analyticsRule.events).contains(
            SyncPromotionBottomSheetSyncFoldersButtonPressedEvent
        )
    }

    @Test
    fun `test that click the secondary button sends the right analytics tracker event`() {
        initComposeRule()
        coroutineScope.launch {
            modalSheetState.show()

            composeRule.onNodeWithText(context.getString(R.string.sync_promotion_bottom_sheet_secondary_button_text))
                .assertExists().assertIsDisplayed().performClick()
            assertThat(analyticsRule.events).contains(
                SyncPromotionBottomSheetLearnMoreButtonPressedEvent
            )
        }
    }

    private fun initComposeRule(
        isFreeAccount: Boolean = false,
    ) {
        composeRule.setContent {
            modalSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                confirmValueChange = {
                    true
                },
                skipHalfExpanded = true,
            )

            coroutineScope = rememberCoroutineScope()

            BottomSheet(
                modalSheetState = modalSheetState,
                sheetBody = {
                    SyncPromotionBottomSheet(
                        upgradeAccountClicked = onUpgrade,
                        isFreeAccount = isFreeAccount,
                    )
                },
                expandedRoundedCorners = true,
            )
        }
    }
}