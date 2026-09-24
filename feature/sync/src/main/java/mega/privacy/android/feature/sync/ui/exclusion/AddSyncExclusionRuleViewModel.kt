package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionValidation
import mega.privacy.android.feature.sync.domain.entity.equalsIgnoringEnabled
import mega.privacy.android.feature.sync.domain.usecase.exclusion.ValidateSyncExclusionValueUseCase
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult
import mega.privacy.android.feature.sync.ui.mapper.exclusion.AddSyncExclusionResultMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionExtensionError
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Holds the drafts of the Add exclusion screen while the user builds a rule.
 *
 * The tabs are kept apart here rather than in the forms, which are stateless bodies, so that
 * showing one tab cannot lose what was typed into the other.
 *
 * @param staged the rules the exclusions list has staged, which a draft is checked against so it
 * cannot repeat one already in force
 */
@HiltViewModel(assistedFactory = AddSyncExclusionRuleViewModel.Factory::class)
internal class AddSyncExclusionRuleViewModel @AssistedInject constructor(
    @Assisted staged: List<SyncExclusionRule>,
    private val resultMapper: AddSyncExclusionResultMapper,
    private val validateSyncExclusionValueUseCase: ValidateSyncExclusionValueUseCase,
) : ViewModel() {

    /**
     * Only rules in force count. Adding one that matches a switched off line turns that line back
     * on rather than repeating it, so it is not an error.
     */
    private val inForce = staged.filter { it.enabled }

    val uiState: StateFlow<AddSyncExclusionRuleUiState>
        field = MutableStateFlow(gated(AddSyncExclusionRuleUiState()))

    /** What the drafts would add, or null while they would add nothing. */
    fun buildResult(): AddSyncExclusionResult? = resultMapper(uiState.value)

    fun onTabSelected(tab: AddExclusionTab) {
        updateState { it.copy(selectedTab = tab) }
    }

    /**
     * Picking a type also drops the condition when the new type carries none, so a rule cannot
     * keep a condition the form has stopped showing.
     */
    fun onTypeOptionSelected(typeOption: SyncExclusionTypeOption) {
        val keepsCondition = typeOption != SyncExclusionTypeOption.Extensions
        updateState { state ->
            state.copy(
                byName = state.byName.copy(
                    typeOption = typeOption,
                    condition = state.byName.condition.takeIf { keepsCondition },
                ),
            )
        }
    }

    fun onConditionSelected(condition: SyncExclusionCondition) {
        updateState { it.copy(byName = it.byName.copy(condition = condition)) }
    }

    fun onNameValueChanged(value: String) {
        updateState { it.copy(byName = it.byName.copy(value = value)) }
    }

    fun onExtensionValueChanged(value: String) {
        updateState { it.copy(byExtension = it.byExtension.copy(value = value)) }
    }

    fun onSizeConditionSelected(condition: SyncSizeCondition) {
        updateSize { it.copy(condition = condition) }
    }

    fun onFirstBoundValueChanged(value: String) {
        updateSize { it.copy(firstBound = it.firstBound.copy(value = value)) }
    }

    fun onFirstBoundUnitSelected(unit: SyncSizeUnit) {
        updateSize { it.copy(firstBound = it.firstBound.copy(unit = unit)) }
    }

    fun onSecondBoundValueChanged(value: String) {
        updateSize { it.copy(secondBound = it.secondBound.copy(value = value)) }
    }

    fun onSecondBoundUnitSelected(unit: SyncSizeUnit) {
        updateSize { it.copy(secondBound = it.secondBound.copy(unit = unit)) }
    }

    private fun updateSize(
        transform: (AddExclusionBySizeFormState) -> AddExclusionBySizeFormState,
    ) {
        updateState { it.copy(bySize = transform(it.bySize)) }
    }

    /**
     * The one way a draft changes, so that a field added later is covered by the confirm gate
     * without its author having to remember it.
     */
    private fun updateState(
        transform: (AddSyncExclusionRuleUiState) -> AddSyncExclusionRuleUiState,
    ) {
        uiState.update { gated(transform(it)) }
    }

    /**
     * [state] with the showing body's error and the confirm gate. A draft showing an error cannot
     * be confirmed, so the button never offers to add what the field says is wrong.
     */
    private fun gated(state: AddSyncExclusionRuleUiState): AddSyncExclusionRuleUiState {
        val result = resultMapper(state)
        val judged = when {
            state.selectedTab != AddExclusionTab.ByName -> state
            state.showsExtensionForm -> state.copy(
                byExtension = state.byExtension.copy(
                    error = extensionError(state.byExtension.extensions),
                ),
            )

            else -> state.copy(
                byName = state.byName.copy(
                    errorRes = sharedR.string.sync_exclusions_add_name_duplicate_error
                        .takeIf { (result as? AddSyncExclusionResult.Rules)?.isInForce() == true },
                ),
            )
        }
        return judged.copy(canConfirm = result != null && !judged.showsError)
    }

    /** The error for the first entry that is wrong, so the user fixes the list in typing order. */
    private fun extensionError(extensions: List<String>): AddExclusionExtensionError? =
        extensions.firstNotNullOfOrNull { extension ->
            val rule = resultMapper.extensionRule(extension)
            when {
                rule == null || !extension.isValidExtension() -> AddExclusionExtensionError.Invalid
                rule.isInForce() -> AddExclusionExtensionError.Duplicate(extension)
                else -> null
            }
        }

    /**
     * Inner dots are allowed so that "tar.gz" keeps working. A space inside an entry is checked
     * here because the validator only rejects whitespace at either end, which trimming has
     * already removed.
     */
    private fun String.isValidExtension(): Boolean =
        none(Char::isWhitespace) &&
                validateSyncExclusionValueUseCase(
                    value = this,
                    matchType = SyncExclusionMatchType.SubtreeName,
                    wildcardsAllowed = false,
                ) == SyncExclusionValidation.Valid

    private fun AddSyncExclusionResult.Rules.isInForce() = rules.any { it.isInForce() }

    /**
     * Compared as the SDK will read the rule back, which drops trailing whitespace from the
     * pattern: otherwise the space a keyboard adds after a suggestion lets a second copy of a rule
     * in force through.
     */
    private fun SyncExclusionRule.isInForce(): Boolean {
        val readBack = copy(pattern = pattern.trimEnd(*SDK_WHITESPACE))
        return inForce.any { it.equalsIgnoringEnabled(readBack) }
    }

    private val AddSyncExclusionRuleUiState.showsError: Boolean
        get() = when {
            selectedTab != AddExclusionTab.ByName -> false
            showsExtensionForm -> byExtension.error != null
            else -> byName.errorRes != null
        }

    @AssistedFactory
    interface Factory {
        fun create(staged: List<SyncExclusionRule>): AddSyncExclusionRuleViewModel
    }

    private companion object {
        /** Whitespace the SDK drops using C-locale `isspace`, narrower than Kotlin's own trim. */
        val SDK_WHITESPACE = charArrayOf(' ', '\t', '\n', '\u000B', '\u000C', '\r')
    }
}
