package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.tabs.MegaFixedTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The Add exclusion screen: the chrome both forms share, and the tab row choosing between them.
 *
 * The chrome is owned here rather than by either form so that the two cannot drift apart, and so
 * that neither form has to know it is one of two.
 */
@Composable
internal fun AddSyncExclusionRuleScreen(
    uiState: AddSyncExclusionRuleUiState,
    onTabSelected: (AddExclusionTab) -> Unit,
    onTypeOptionSelected: (SyncExclusionTypeOption) -> Unit,
    onConditionSelected: (SyncExclusionCondition) -> Unit,
    onNameValueChanged: (String) -> Unit,
    onExtensionValueChanged: (String) -> Unit,
    onSizeConditionSelected: (SyncSizeCondition) -> Unit,
    onFirstBoundValueChanged: (String) -> Unit,
    onFirstBoundUnitSelected: (SyncSizeUnit) -> Unit,
    onSecondBoundValueChanged: (String) -> Unit,
    onSecondBoundUnitSelected: (SyncSizeUnit) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(ADD_EXCLUSION_TOP_BAR_TAG),
                title = stringResource(sharedR.string.sync_exclusions_add_title),
                navigationType = AppBarNavigationType.Close(onDismiss),
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(ADD_EXCLUSION_BUTTON_GROUP_TAG),
                buttonGroup = listOf(
                    {
                        Button.TextOnlyButtonM3(
                            text = stringResource(sharedR.string.general_dialog_cancel_button),
                            onClick = onDismiss,
                        )
                    },
                    {
                        Button.PrimaryButtonM3(
                            text = stringResource(sharedR.string.sync_exclusions_add_confirm),
                            onClick = onConfirm,
                        )
                    },
                ),
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                MegaFixedTabRow(
                    modifier = Modifier.testTag(ADD_EXCLUSION_TAB_ROW_TAG),
                    tabIndex = uiState.selectedTab.ordinal,
                    items = AddExclusionTab.entries.map {
                        TabItems(title = stringResource(it.labelRes))
                    },
                    onClick = { index -> onTabSelected(AddExclusionTab.entries[index]) },
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (uiState.selectedTab) {
                        AddExclusionTab.ByName -> if (uiState.showsExtensionForm) {
                            AddExclusionByExtensionForm(
                                state = uiState.byExtension,
                                onTypeOptionSelected = onTypeOptionSelected,
                                onValueChanged = onExtensionValueChanged,
                            )
                        } else {
                            AddExclusionByNameForm(
                                state = uiState.byName,
                                onTypeOptionSelected = onTypeOptionSelected,
                                onConditionSelected = onConditionSelected,
                                onValueChanged = onNameValueChanged,
                            )
                        }

                        AddExclusionTab.BySize -> AddExclusionBySizeForm(
                            state = uiState.bySize,
                            onConditionSelected = onSizeConditionSelected,
                            onFirstBoundValueChanged = onFirstBoundValueChanged,
                            onFirstBoundUnitSelected = onFirstBoundUnitSelected,
                            onSecondBoundValueChanged = onSecondBoundValueChanged,
                            onSecondBoundUnitSelected = onSecondBoundUnitSelected,
                        )
                    }
                }
            }
        },
    )
}

internal const val ADD_EXCLUSION_TOP_BAR_TAG = "add_exclusion:top_bar"
internal const val ADD_EXCLUSION_TAB_ROW_TAG = "add_exclusion:tab_row"
internal const val ADD_EXCLUSION_BUTTON_GROUP_TAG = "add_exclusion:button_group"

private class AddSyncExclusionRuleUiStateProvider :
    PreviewParameterProvider<AddSyncExclusionRuleUiState> {
    override val values = sequenceOf(
        AddSyncExclusionRuleUiState(),
        AddSyncExclusionRuleUiState(
            selectedTab = AddExclusionTab.ByName,
            byName = AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.EndingWith,
                value = "tmp",
            ),
        ),
        AddSyncExclusionRuleUiState(
            selectedTab = AddExclusionTab.BySize,
            bySize = AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            ),
        ),
    )
}

@CombinedThemePreviews
@Composable
private fun AddSyncExclusionRuleScreenPreview(
    @PreviewParameter(AddSyncExclusionRuleUiStateProvider::class)
    uiState: AddSyncExclusionRuleUiState,
) {
    AndroidThemeForPreviews {
        AddSyncExclusionRuleScreen(
            uiState = uiState,
            onTabSelected = {},
            onTypeOptionSelected = {},
            onConditionSelected = {},
            onNameValueChanged = {},
            onExtensionValueChanged = {},
            onSizeConditionSelected = {},
            onFirstBoundValueChanged = {},
            onFirstBoundUnitSelected = {},
            onSecondBoundValueChanged = {},
            onSecondBoundUnitSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
