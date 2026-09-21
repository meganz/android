package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.test.weblate.WeblateContentDescriptionScreenshot
import mega.privacy.android.core.test.weblate.WeblateSnackbarScreenshot
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionChipMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionPatternMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionRowMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionSupportingTextMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageSyncExclusionsUiState
import mega.privacy.android.shared.resources.R as sharedR

class ManageSyncExclusionsScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenWithDefaults() {
        AndroidThemeForPreviews { DefaultsScreen() }
    }

    @Composable
    private fun DefaultsScreen() {
        val parsed = ParsedMegaIgnore(
            entries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) },
        )
        Screen(
            ManageSyncExclusionsUiState(
                loaded = parsed,
                staged = parsed,
                syncName = "Documents",
                isWritable = true,
                isLoading = false,
            ),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenWithSizeBounds() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES.first()),
                MegaIgnoreEntry.SizeSmallerEntry(bytes = 1_024L),
                MegaIgnoreEntry.SizeLargerEntry(bytes = 2_147_483_648L),
            ),
        )
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    loaded = parsed,
                    staged = parsed,
                    syncName = "Documents",
                    isWritable = true,
                    isLoading = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenEmpty() {
        val parsed = ParsedMegaIgnore(entries = emptyList())
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    loaded = parsed,
                    staged = parsed,
                    syncName = "Documents",
                    isWritable = true,
                    isLoading = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenSizeBiggerThan() {
        SizeBoundsScreen(MegaIgnoreEntry.SizeLargerEntry(bytes = 1_073_741_824L))
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenSizeSmallerThan() {
        SizeBoundsScreen(MegaIgnoreEntry.SizeSmallerEntry(bytes = 1_024L))
    }

    /**
     * A lower bound above the upper one, which the SDK reads as excluding what lies between them
     * rather than what lies outside.
     */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenSizeBetween() {
        SizeBoundsScreen(
            MegaIgnoreEntry.SizeSmallerEntry(bytes = 2_147_483_648L),
            MegaIgnoreEntry.SizeLargerEntry(bytes = 1_024L),
        )
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenRejectedBySdk() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(
                MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES.first()),
                MegaIgnoreEntry.InvalidEntry(
                    line = "nonsense",
                    reason = InvalidLineReason.MissingSeparator,
                ),
            ),
        )
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    loaded = parsed,
                    staged = parsed,
                    syncName = "Documents",
                    isWritable = true,
                    isLoading = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenOnlyUnshownRules() {
        val parsed = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.PreservedEntry("-r:^backup_.*")),
        )
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    loaded = parsed,
                    staged = parsed,
                    syncName = "Documents",
                    isWritable = true,
                    isLoading = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsScreenLoadFailed() {
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    syncName = "Documents",
                    isLoading = false,
                    hasLoadFailed = true,
                ),
            )
        }
    }

    /** The confirmation the Add flow will raise once SAT-2470 returns its result. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsAddedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.sync_exclusions_manage_snackbar_added),
            ) {
                DefaultsScreen()
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsSaveFailedSnackbar() {
        AndroidThemeForPreviews {
            WeblateSnackbarScreenshot(
                snackbarText = stringResource(sharedR.string.sync_exclusions_manage_save_failed),
            ) {
                DefaultsScreen()
            }
        }
    }

    /** The chip's dismiss control has a spoken label and no visible text of its own. */
    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageSyncExclusionsRemoveExtensionDescription() {
        AndroidThemeForPreviews {
            WeblateContentDescriptionScreenshot(
                description = stringResource(
                    sharedR.string.sync_exclusions_manage_remove_extension,
                    "tmp",
                ),
                tooltipAlignment = Alignment.TopStart,
                tooltipOffset = DpOffset(x = 64.dp, y = 166.dp),
            ) {
                DefaultsScreen()
            }
        }
    }

    @Composable
    private fun SizeBoundsScreen(vararg bounds: MegaIgnoreEntry) {
        val parsed = ParsedMegaIgnore(
            entries = listOf(MegaIgnoreEntry.RuleEntry(MegaIgnoreDefaults.RULES.first())) +
                    bounds.toList(),
        )
        AndroidThemeForPreviews {
            Screen(
                ManageSyncExclusionsUiState(
                    loaded = parsed,
                    staged = parsed,
                    syncName = "Documents",
                    isWritable = true,
                    isLoading = false,
                ),
            )
        }
    }

    @Composable
    private fun Screen(uiState: ManageSyncExclusionsUiState) {
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
        )
    }

    private val chipMapper = SyncExclusionChipMapper(SyncExclusionPatternMapper())

    private val rowMapper = SyncExclusionRowMapper(
        patternMapper = SyncExclusionPatternMapper(),
        supportingTextMapper = SyncExclusionSupportingTextMapper(),
    )
}
