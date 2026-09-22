package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import javax.inject.Inject

/**
 * Reads the change an Edit exclusion form has authored, the way back from
 * [SyncExclusionEditPrefillMapper].
 *
 * Only the two fields that form renders are set. Every other field is left unstated so the rule
 * keeps it, because an edit must not drop grammar the screen never showed the user.
 */
internal class SyncExclusionEditResultMapper @Inject constructor(
    private val patternMapper: SyncExclusionPatternMapper,
) {

    /**
     * The change [form] describes, or null while it describes none: a type or condition still
     * unpicked, or a value that is blank, which no pattern can be built from.
     */
    operator fun invoke(form: AddExclusionByNameFormState): SyncExclusionEdit? {
        val target = form.typeOption?.target ?: return null
        val condition = form.condition ?: return null
        if (form.value.isBlank()) return null
        return SyncExclusionEdit(
            pattern = patternMapper(condition, form.value),
            target = target,
        )
    }

    /**
     * The node types the chosen entry stands for, or null for the extensions entry, which names a
     * mode of the Add form rather than a target the grammar has.
     */
    private val SyncExclusionTypeOption.target: SyncExclusionTarget?
        get() = when (this) {
            SyncExclusionTypeOption.Files -> SyncExclusionTarget.Files
            SyncExclusionTypeOption.Folders -> SyncExclusionTarget.Directories
            SyncExclusionTypeOption.FilesAndFolders -> SyncExclusionTarget.All
            SyncExclusionTypeOption.Extensions -> null
        }
}
