package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionClass
import javax.inject.Inject

/**
 * Reinstates the SDK's predefined exclusions in a `.megaignore`, replacing the exclusions this app
 * owns and leaving every other line of the file as it was.
 */
internal class RestoreDefaultSyncExclusionsUseCase @Inject constructor() {

    /**
     * Writes [MegaIgnoreDefaults.RULES] at the top of [current] and drops the exclusion rules it
     * held, keeping every other line in its original relative order.
     *
     * Everything the exclusions UI both owns and draws is reset: the excluding
     * [MegaIgnoreEntry.RuleEntry] items behind the name and extension lists, and the size bounds
     * behind the size row. The defaults carry no size bound, so restoring has to clear one for the
     * list to come back to what the SDK sets up.
     *
     * Everything the UI cannot show is kept, because the user cannot have authored it here and
     * would not see it go: an including rule, since the list cannot express a rule's class, so a
     * hand added `+:*.tmp` has to survive a reset of the exclusions it was written to override;
     * and lines this app does not model or the SDK refuses.
     *
     * The defaults go first, rather than into the position of the rule they replace, so that
     * everything the user or another client authored sits after them and wins under the SDK's last
     * match wins evaluation. Anchoring them at the first rule instead made that outcome depend on
     * an unrelated line: a file whose first entry is a regex include would take the defaults below
     * it and let the default `-:*.tmp` defeat the include, while the same file without a second
     * line to anchor at would not.
     *
     * A [MegaIgnoreEntry.InvalidEntry] survives, so [ParsedMegaIgnore.isRejectedBySdk] stays true
     * and none of the restored defaults are actually in effect. Restoring cannot repair such a
     * file, and a caller offering it must still say so.
     */
    operator fun invoke(current: ParsedMegaIgnore): ParsedMegaIgnore = current.copy(
        entries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) } +
                current.entries.filterNot { it.isDrawnByTheExclusionsUi() }
    )

    // exhaustive rather than an else, so a new kind of entry has to be placed on one side or the
    // other instead of silently surviving a reset
    private fun MegaIgnoreEntry.isDrawnByTheExclusionsUi(): Boolean = when (this) {
        is MegaIgnoreEntry.RuleEntry -> rule.exclusionClass == SyncExclusionClass.Exclude
        is MegaIgnoreEntry.SizeSmallerEntry -> true
        is MegaIgnoreEntry.SizeLargerEntry -> true
        is MegaIgnoreEntry.PreservedEntry -> false
        is MegaIgnoreEntry.InvalidEntry -> false
    }
}
