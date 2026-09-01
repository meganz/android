package mega.privacy.android.feature.sync.domain.entity

/**
 * The name exclusions the SDK seeds into a new sync's `.megaignore`, and the test for whether a
 * rule is one of them.
 *
 * The SDK keeps these as `DefaultFilterChain::mPredefinedNameExclusions` and writes each one as
 * `-:<pattern>`, so a default carries no target, type or strategy letter: it applies to every node
 * type, matches a name anywhere in the subtree, and is case insensitive.
 *
 * The list is a private static member on the C++ side with no Java binding, so neither the app nor
 * a unit test can read it out of the prebuilt SDK. It is copied here and pinned by a fixture in
 * `MegaIgnoreDefaultsTest`, which means an SDK bump that moves the defaults has to be reconciled by
 * hand rather than caught automatically.
 *
 * The SDK only consults the list while `.megaignore.default` is absent: it writes that file once
 * from the list and thereafter copies the file verbatim into every new sync, never regenerating it.
 * On an install predating a change to the list the files on disk therefore keep the older set, and
 * [isDefault] will disagree with them.
 *
 * Reading `.megaignore.default` here instead was rejected: on Android it resolves under the
 * app-private data directory, and the SDK only creates it lazily, so on a fresh install whose first
 * sync is created *with* exclusions there is no file to read.
 */
object MegaIgnoreDefaults {

    /**
     * The predefined exclusions, in the SDK's own order.
     *
     * Held as rules rather than as bare patterns so that no caller has to decide again which
     * grammar fields a default carries; rebuilding a rule from its pattern at a call site is how
     * the other fields get silently dropped.
     */
    val RULES: List<SyncExclusionRule> = listOf(
        "Thumbs.db",
        "desktop.ini",
        "~*",
        ".*",
        "*~.*",
        "*.crdownload",
        "*.sb-????????-??????",
        "*.tmp",
    ).map { pattern ->
        SyncExclusionRule(
            pattern = pattern,
            matchType = SyncExclusionMatchType.SubtreeName,
        )
    }

    /**
     * Whether [rule] is one of the predefined exclusions.
     *
     * [SyncExclusionRule.enabled] is deliberately left out of the comparison: a default the user
     * has switched off is still a default, held in the file as a commented out line, and the list
     * has to keep presenting it as one.
     *
     * The pattern is matched exactly, not case insensitively. A rule is a default because it is the
     * SDK's literal seed line, not because it happens to match the same files, so a hand typed
     * `thumbs.db` stays a rule the user owns and can delete.
     *
     * Comparing whole rules holds only while a rule carries nothing but grammar. [ParsedMegaIgnore]
     * has already settled that a rule is identified by its index rather than by anything it carries,
     * so nothing should add a field here; if something does, this silently stops recognising
     * defaults read from a file.
     */
    fun isDefault(rule: SyncExclusionRule): Boolean = rule.copy(enabled = true) in RULES
}
