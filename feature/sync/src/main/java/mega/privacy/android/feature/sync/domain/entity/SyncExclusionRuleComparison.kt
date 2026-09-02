package mega.privacy.android.feature.sync.domain.entity

/**
 * Whether two rules say the same thing about the same paths, disregarding whether either of them is
 * currently in effect.
 *
 * A switched off rule survives as a commented out line, so adding it again has to find the disabled
 * copy rather than append a second one: the SDK evaluates filters last match wins, and two lines
 * for one rule leave a list the user cannot reconcile with what is being excluded.
 */
internal fun SyncExclusionRule.equalsIgnoringEnabled(other: SyncExclusionRule): Boolean =
    copy(enabled = other.enabled) == other
