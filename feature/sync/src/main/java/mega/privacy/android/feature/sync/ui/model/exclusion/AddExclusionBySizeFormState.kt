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

    /** The bounds the chosen condition actually reads, in the order they are drawn. */
    val activeBounds: List<SyncSizeBoundInput>
        get() = when {
            condition == null -> emptyList()
            showsSecondBound -> listOf(firstBound, secondBound)
            else -> listOf(firstBound)
        }
}
