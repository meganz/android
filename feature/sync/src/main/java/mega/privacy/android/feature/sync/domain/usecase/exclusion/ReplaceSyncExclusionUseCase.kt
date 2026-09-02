package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget
import mega.privacy.android.feature.sync.domain.entity.entriesIndexOfRule
import mega.privacy.android.feature.sync.domain.entity.equalsIgnoringEnabled
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import javax.inject.Inject

/**
 * Applies an edit to one rule of a `.megaignore`.
 */
internal class ReplaceSyncExclusionUseCase @Inject constructor() {

    /**
     * The edit is merged onto the rule the file already holds, so every field [edit] leaves null
     * comes back unchanged. A rule may have been authored by another client and carry grammar this
     * app never renders, and a screen that cannot show a field cannot be taken to have chosen one
     * for it.
     *
     * [SyncExclusionTarget.Symlinks] stays whatever the source rule has, since nothing here offers
     * symlinks. An edit merely repeating the source's own symlink target is accepted, since a form
     * prefilled from the rule sends every field back.
     *
     * The duplicate guard asks only whether this edit is what introduces the duplicate, so
     * switching one of a file's legitimately repeated rules on or off stays possible.
     *
     * @param current the file as parsed
     * @param ruleIndex the rule's position in [ParsedMegaIgnore.rules]
     * @param expected the rule the caller read at [ruleIndex], which the index has to still be
     * pointing at in a file every other client of the account can edit. Its
     * [SyncExclusionRule.enabled] is held against the file only when [edit] sets that field, since
     * otherwise the state is taken from the file rather than from the caller
     * @param edit the fields to change, each null field keeping the source rule's value. A pattern
     * is taken verbatim
     * @return [current] with that one rule replaced at its own line, every other line still in the
     * position it held, or [SyncExclusionEditError.StaleRuleTarget],
     * [SyncExclusionEditError.UnsupportedTarget] or [SyncExclusionEditError.DuplicateRule]
     */
    operator fun invoke(
        current: ParsedMegaIgnore,
        ruleIndex: Int,
        expected: SyncExclusionRule,
        edit: SyncExclusionEdit,
    ): Result<ParsedMegaIgnore> {
        val entriesIndex = current.entriesIndexOfRule(ruleIndex, expected)
            ?: return Result.failure(SyncExclusionEditError.StaleRuleTarget(ruleIndex))
        val source = (current.entries[entriesIndex] as MegaIgnoreEntry.RuleEntry).rule
        if (edit.enabled != null && source.enabled != expected.enabled) {
            return Result.failure(SyncExclusionEditError.StaleRuleTarget(ruleIndex))
        }
        if (edit.target == SyncExclusionTarget.Symlinks &&
            source.target != SyncExclusionTarget.Symlinks
        ) {
            return Result.failure(
                SyncExclusionEditError.UnsupportedTarget(SyncExclusionTarget.Symlinks)
            )
        }
        val replacement = source.copy(
            pattern = edit.pattern ?: source.pattern,
            exclusionClass = edit.exclusionClass ?: source.exclusionClass,
            target = if (source.target == SyncExclusionTarget.Symlinks) {
                source.target
            } else {
                edit.target ?: source.target
            },
            matchType = edit.matchType ?: source.matchType,
            strategy = edit.strategy ?: source.strategy,
            enabled = edit.enabled ?: source.enabled,
        )
        if (!replacement.equalsIgnoringEnabled(source) &&
            current.holdsRuleOtherThanAt(entriesIndex, replacement)
        ) {
            return Result.failure(SyncExclusionEditError.DuplicateRule(replacement.pattern))
        }
        return Result.success(
            current.copy(
                entries = current.entries.mapIndexed { index, entry ->
                    if (index == entriesIndex) MegaIgnoreEntry.RuleEntry(replacement) else entry
                },
            )
        )
    }

    private fun ParsedMegaIgnore.holdsRuleOtherThanAt(
        entriesIndex: Int,
        rule: SyncExclusionRule,
    ): Boolean = entries.withIndex().any { (index, entry) ->
        index != entriesIndex &&
                entry is MegaIgnoreEntry.RuleEntry &&
                entry.rule.equalsIgnoringEnabled(rule)
    }
}
