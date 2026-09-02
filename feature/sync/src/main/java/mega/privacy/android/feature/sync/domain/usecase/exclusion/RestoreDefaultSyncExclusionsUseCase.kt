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
     * Only an excluding [MegaIgnoreEntry.RuleEntry] is this app's to reset. An including one is
     * kept, because the list this app presents cannot express a rule's class at all: an include is
     * therefore not a rule the user can see here, and a hand added `+:*.tmp` that puts a file type
     * back into the sync has to survive a reset of the exclusions it was written to override. Size
     * bounds, lines this app does not model and lines the SDK refuses are kept for the same reason.
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
                current.entries.filterNot { it.isExclusionRule() }
    )

    private fun MegaIgnoreEntry.isExclusionRule(): Boolean =
        this is MegaIgnoreEntry.RuleEntry && rule.exclusionClass == SyncExclusionClass.Exclude
}
