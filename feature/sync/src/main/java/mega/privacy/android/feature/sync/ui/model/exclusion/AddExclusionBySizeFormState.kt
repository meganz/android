package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * One typed size bound: the digits as typed, and the unit they are read in.
 *
 * Each bound carries its own unit. Sharing one unit across a range cannot express the mixed pairs
 * the design draws, and reading an existing range back through a shared unit rounds the smaller
 * bound away.
 *
 * @property value the digits as typed, kept as text so a half-typed bound survives recomposition
 */
internal data class SyncSizeBoundInput(
    val value: String = "",
    val unit: SyncSizeUnit = SyncSizeUnit.Megabytes,
) {

    /**
     * The bound in bytes, or `null` while it is empty, not a positive number, or too large to
     * hold. A bound of zero excludes every file, so it is not a bound the form can express.
     */
    val bytes: Long?
        get() = value.toLongOrNull()?.takeIf { it > 0 }?.let { unit.toBytes(it) }

    val isEmpty: Boolean
        get() = value.isEmpty()
}

/**
 * What the Add exclusion by-size form shows.
 *
 * The two bounds are positional rather than named after the lines they become, because the
 * condition decides which line each one writes and the error copy names them by position. Under
 * [SyncSizeCondition.NotBetween] the first bound is the lower one.
 */
internal data class AddExclusionBySizeFormState(
    val condition: SyncSizeCondition? = null,
    val firstBound: SyncSizeBoundInput = SyncSizeBoundInput(),
    val secondBound: SyncSizeBoundInput = SyncSizeBoundInput(),
) {

    /** Nothing is typed before a condition names how many bounds the rule takes. */
    val showsBounds: Boolean
        get() = condition != null

    /** The second bound and the conjunction before it belong to a range only. */
    val showsSecondBound: Boolean
        get() = condition?.usesSecondBound == true

    /**
     * Whether a bound the condition reads is typed but is not a size a rule can use: zero, or too
     * large to hold in bytes. The design draws no copy for either, so this refuses the rule
     * without naming a reason.
     */
    val hasUnusableBound: Boolean
        get() = activeBounds.any { !it.isEmpty && it.bytes == null }

    /**
     * Whether the range reads downwards. Equal bounds count: "not between 1 KB and 1 KB" leaves
     * out every file. Only two readable sizes are compared, so the copy cannot claim an order
     * about a bound that has no size — [hasUnusableBound] carries that instead.
     */
    val showsRangeError: Boolean
        get() {
            if (!showsSecondBound) return false
            val first = firstBound.bytes ?: return false
            val second = secondBound.bytes ?: return false
            return first >= second
        }

    /** The bounds the chosen condition actually reads, in the order they are drawn. */
    val activeBounds: List<SyncSizeBoundInput>
        get() = when {
            condition == null -> emptyList()
            showsSecondBound -> listOf(firstBound, secondBound)
            else -> listOf(firstBound)
        }
}
