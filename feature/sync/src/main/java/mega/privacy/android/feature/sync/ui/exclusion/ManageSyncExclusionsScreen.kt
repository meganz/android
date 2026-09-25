package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageSyncExclusionsUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.RestoreDefaultsMenuAction
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionChipUiItem
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowDescription
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem
import mega.privacy.android.feature.sync.ui.views.ExclusionRuleRow
import mega.privacy.android.feature.sync.ui.views.ExclusionSizeRow
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsAddButtonPressedEvent

/**
 * The exclusions of one sync, in the three groups the design fixes: extensions as chips, then
 * name rules as rows, then the file's size bounds.
 *
 * Stateless. Every edit is staged by the ViewModel and written once on save, so the screen reports
 * changes rather than applying them.
 */
@Composable
internal fun ManageSyncExclusionsScreen(
    uiState: ManageSyncExclusionsUiState,
    chips: List<SyncExclusionChipUiItem>,
    rows: List<SyncExclusionRowUiItem>,
    onBackPressed: () -> Unit,
    onAddClicked: () -> Unit,
    onSaveClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    onRuleClicked: (SyncExclusionRowUiItem) -> Unit,
    onExtensionRemoved: (SyncExclusionChipUiItem) -> Unit,
    onSizeClicked: () -> Unit,
    onRestoreDefaultsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.sync_exclusions_manage_title),
                subtitle = uiState.syncName,
                navigationType = AppBarNavigationType.Back(onBackPressed),
                // an action carrying no icon is what the design system puts in the overflow
                // menu, which is where the design draws this one
                actions = if (uiState.canRestoreDefaults) {
                    listOf(
                        MenuActionWithClick(
                            RestoreDefaultsMenuAction,
                            onRestoreDefaultsClicked,
                        ),
                    )
                } else {
                    emptyList()
                },
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                buttonGroup = listOf(
                    {
                        Button.SecondaryButtonM3(
                            text = stringResource(sharedR.string.sync_exclusions_manage_add),
                            onClick = {
                                Analytics.tracker.trackEvent(
                                    AndroidSyncExclusionsAddButtonPressedEvent
                                )
                                onAddClicked()
                            },
                            enabled = uiState.canAddExclusion,
                        )
                    },
                    {
                        Button.PrimaryButtonM3(
                            text = stringResource(sharedR.string.sync_exclusions_manage_save),
                            onClick = onSaveClicked,
                            enabled = uiState.canSave,
                            isLoading = uiState.isSaving,
                        )
                    },
                ),
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )

                    uiState.hasLoadFailed -> ExclusionsLoadError(onRetry = onRetryClicked)

                    // a file the SDK refuses excludes nothing, so its rules must not be drawn as
                    // though they were in force
                    uiState.isRejected -> ExclusionsRejected()

                    uiState.hasNothingToShow -> ExclusionsEmptyState(
                        hasUnshownRules = uiState.hasUnshownRules,
                    )

                    else -> ExclusionsList(
                        chips = chips,
                        rows = rows,
                        size = uiState.staged?.size,
                        onRuleClicked = onRuleClicked,
                        onExtensionRemoved = onExtensionRemoved,
                        onSizeClicked = onSizeClicked,
                    )
                }
            }
        },
    )
}

@Composable
private fun ExclusionsList(
    chips: List<SyncExclusionChipUiItem>,
    rows: List<SyncExclusionRowUiItem>,
    size: SyncSizeExclusion?,
    onRuleClicked: (SyncExclusionRowUiItem) -> Unit,
    onExtensionRemoved: (SyncExclusionChipUiItem) -> Unit,
    onSizeClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sizeTexts = sizeRowTexts(size)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp),
    ) {
        if (chips.isNotEmpty()) {
            item(key = SECTION_EXTENSIONS) {
                ExclusionSectionHeader(
                    text = stringResource(sharedR.string.sync_exclusions_manage_section_extensions),
                )
                MegaDivider(dividerType = DividerType.FullSize)
            }
            item(key = CHIPS) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chips.forEach { chip ->
                        ExclusionExtensionChip(
                            extension = chip.extension,
                            onRemove = { onExtensionRemoved(chip) },
                        )
                    }
                }
            }
        }

        if (rows.isNotEmpty()) {
            item(key = SECTION_NAMES) {
                ExclusionSectionHeader(
                    text = stringResource(sharedR.string.sync_exclusions_manage_section_names),
                    modifier = Modifier.padding(top = sectionGap(chips.isNotEmpty())),
                )
                MegaDivider(dividerType = DividerType.FullSize)
            }
            // keyed by the rule's position in the file, which is unique even when two rules differ
            // only by whether they are enabled
            items(count = rows.size, key = { rows[it].ruleIndex }) { index ->
                val row = rows[index]
                ExclusionRuleRow(
                    pattern = row.pattern(),
                    supportingText = row.supportingText(),
                    onClick = { onRuleClicked(row) },
                )
                MegaDivider(dividerType = DividerType.FullSize)
            }
        }

        if (sizeTexts.isNotEmpty()) {
            item(key = SECTION_SIZE) {
                ExclusionSectionHeader(
                    text = stringResource(sharedR.string.sync_exclusions_manage_section_size),
                    modifier = Modifier.padding(top = sectionGap(chips.isNotEmpty() || rows.isNotEmpty())),
                )
                MegaDivider(dividerType = DividerType.FullSize)
            }
            items(count = sizeTexts.size, key = { sizeTexts[it] }) { index ->
                ExclusionSizeRow(text = sizeTexts[index], onClick = onSizeClicked)
                MegaDivider(dividerType = DividerType.FullSize)
            }
        }
    }
}

private fun sectionGap(followsAnotherSection: Boolean) =
    if (followsAnotherSection) 12.dp else 0.dp

/**
 * The size bounds as the sentences the design draws.
 *
 * A file declaring both bounds states one range rather than two limits, which is why the pair is
 * read together instead of a row per bound. Which range it states depends on their order: the SDK
 * reads a lower bound above the upper one as an in-range exclusion, leaving out what lies between
 * them rather than what lies outside, so the two cases cannot share a sentence.
 */
@Composable
private fun sizeRowTexts(size: SyncSizeExclusion?): List<String> {
    if (size == null) return emptyList()
    val context = LocalContext.current
    val smaller = size.excludeSmallerThanBytes
    val larger = size.excludeLargerThanBytes
    return when {
        smaller != null && larger != null -> listOf(
            if (smaller > larger) {
                stringResource(
                    sharedR.string.sync_exclusions_manage_size_between,
                    formatFileSize(larger, context),
                    formatFileSize(smaller, context),
                )
            } else {
                stringResource(
                    sharedR.string.sync_exclusions_manage_size_not_between,
                    formatFileSize(smaller, context),
                    formatFileSize(larger, context),
                )
            },
        )

        larger != null -> listOf(
            stringResource(
                sharedR.string.sync_exclusions_manage_size_bigger_than,
                formatFileSize(larger, context),
            ),
        )

        smaller != null -> listOf(
            stringResource(
                sharedR.string.sync_exclusions_manage_size_smaller_than,
                formatFileSize(smaller, context),
            ),
        )

        else -> emptyList()
    }
}

private fun SyncExclusionRowUiItem.pattern(): String = when (val description = description) {
    is SyncExclusionRowDescription.Described -> description.value
    is SyncExclusionRowDescription.Undescribed -> description.pattern
}

@Composable
private fun SyncExclusionRowUiItem.supportingText(): String =
    when (val description = description) {
        is SyncExclusionRowDescription.Described -> stringResource(description.supportingTextRes)
        is SyncExclusionRowDescription.Undescribed -> ""
    }

private const val SECTION_EXTENSIONS = "section_extensions"
private const val SECTION_NAMES = "section_names"
private const val SECTION_SIZE = "section_size"
private const val CHIPS = "chips"
