package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * The three entries of the by-size "Exclude files" sheet.
 *
 * Size rules are expressed by the `exclude-smaller` and `exclude-larger` lines, which the SDK
 * applies to files only, so no target is chosen alongside them.
 */
internal enum class SyncSizeCondition {

    /** Leaves out files above one bound. */
    BiggerThan,

    /** Leaves out files below one bound. */
    SmallerThan,

    /** Leaves out files outside a range, which is the only condition using both bounds. */
    NotBetween,
    ;

    /** Whether a second bound is typed, and drawn after the conjunction. */
    val usesSecondBound: Boolean
        get() = this == NotBetween
}
