package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.menu.TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionChipMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionPatternMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionRowMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionSupportingTextMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageSyncExclusionsUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.TEST_TAG_RESTORE_DEFAULTS_ACTION
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class ManageSyncExclusionsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var restored = 0

    @Test
    fun `test that the overflow menu offers restoring the defaults`() {
        setContent()

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).performClick()

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_manage_restore_defaults))
            .assertExists()
    }

    @Test
    fun `test that choosing restore defaults reports it once`() {
        setContent()

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).performClick()
        composeTestRule.onNodeWithTag(TEST_TAG_RESTORE_DEFAULTS_ACTION).performClick()

        assertThat(restored).isEqualTo(1)
    }

    /**
     * The four states that leave nothing to reset. Offering the action on any of them would
     * report a restore the user cannot see, or one that can never reach the folder.
     *
     * Each keeps a staged model the first two would not have in practice, so that the clause
     * under test is the one the name claims rather than the absent model standing in for it.
     */
    @Test
    fun `test that the overflow menu is absent while the exclusions are being read`() {
        setContent(loadedState(defaults).copy(isLoading = true))

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that the overflow menu is absent when the exclusions could not be read`() {
        setContent(loadedState(defaults).copy(hasLoadFailed = true))

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that the overflow menu is absent when the root cannot be written`() {
        setContent(loadedState(defaults).copy(isWritable = false))

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that the overflow menu is absent when the sync refuses the file`() {
        val rejected = ParsedMegaIgnore(
            entries = defaults.entries + MegaIgnoreEntry.InvalidEntry(
                line = "-x:nonsense",
                reason = InvalidLineReason.UnexpectedFlagCharacter,
            ),
        )
        setContent(loadedState(rejected))

        composeTestRule.onNodeWithTag(TAG_TOP_APP_BAR_ACTIONS_SHOW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that adding is offered once the exclusions have been read`() {
        setContent()

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_manage_add))
            .assertIsEnabled()
    }

    @Test
    fun `test that adding is not offered while there is no file to stage a rule onto`() {
        setContent(ManageSyncExclusionsUiState(syncName = "Documents", hasLoadFailed = true))

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_manage_add))
            .assertIsNotEnabled()
    }

    private fun setContent(uiState: ManageSyncExclusionsUiState = loadedState(defaults)) {
        composeTestRule.setContent {
            val staged = uiState.staged
            ManageSyncExclusionsScreen(
                uiState = uiState,
                chips = staged?.let(chipMapper::invoke).orEmpty(),
                rows = staged?.let(rowMapper::invoke).orEmpty(),
                onBackPressed = {},
                onAddClicked = {},
                onSaveClicked = {},
                onRetryClicked = {},
                onRuleClicked = {},
                onExtensionRemoved = {},
                onSizeClicked = {},
                onRestoreDefaultsClicked = { restored++ },
            )
        }
    }

    private fun loadedState(exclusions: ParsedMegaIgnore) = ManageSyncExclusionsUiState(
        loaded = exclusions,
        staged = exclusions,
        syncName = "Documents",
        isWritable = true,
        isLoading = false,
    )

    private fun string(resId: Int) = composeTestRule.activity.getString(resId)

    private val defaults = ParsedMegaIgnore(
        entries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) },
    )

    private val chipMapper = SyncExclusionChipMapper(SyncExclusionPatternMapper())

    private val rowMapper = SyncExclusionRowMapper(
        patternMapper = SyncExclusionPatternMapper(),
        supportingTextMapper = SyncExclusionSupportingTextMapper(),
    )
}
