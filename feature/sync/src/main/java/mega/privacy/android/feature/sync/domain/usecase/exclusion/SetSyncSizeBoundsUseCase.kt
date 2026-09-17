package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.InvalidLineReason
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import javax.inject.Inject

/**
 * Sets, changes or clears the size bounds of a `.megaignore`, leaving every other line where it was.
 *
 * The bounds belong to the file rather than to any rule, so they are replaced as a pair: passing a
 * [SyncSizeExclusion] with neither bound is how a caller clears them.
 *
 * A bound too large to hold in a `Long` is not modelled as a bound at all and so survives this,
 * leaving a file the app reports as unbounded while the SDK still enforces a limit. That gap is the
 * parser's rather than this transform's, but clearing cannot close it.
 */
internal class SetSyncSizeBoundsUseCase @Inject constructor() {

    /**
     * @param current the file as parsed
     * @param bounds the bounds the file should end up declaring. A null bound is not left alone, it
     * is removed — the pair is what the screen edits, so a caller able to leave half of it behind
     * could clear a range into a rule the user never chose
     * @return [current] declaring exactly [bounds], or [SyncExclusionEditError.UnusableSizeBound] /
     * [SyncExclusionEditError.EqualSizeBounds]
     */
    operator fun invoke(
        current: ParsedMegaIgnore,
        bounds: SyncSizeExclusion,
    ): Result<ParsedMegaIgnore> {
        val smaller = bounds.excludeSmallerThanBytes
        val larger = bounds.excludeLargerThanBytes

        listOfNotNull(smaller, larger).firstOrNull { it <= 0 }?.let {
            return Result.failure(SyncExclusionEditError.UnusableSizeBound(it))
        }
        if (smaller != null && smaller == larger) {
            return Result.failure(SyncExclusionEditError.EqualSizeBounds(smaller))
        }

        val declared = listOfNotNull(
            smaller?.let { MegaIgnoreEntry.SizeSmallerEntry(it) },
            larger?.let { MegaIgnoreEntry.SizeLargerEntry(it) },
        )
        val kept = current.entries.filterNot(::isBoundLine)
        // the bounds go back where the file already kept them, because a bound line that moves
        // past one this app cannot model changes which of them the SDK holds when it reads it
        val at = current.entries.indexOfFirst(::isBoundLine).takeIf { it >= 0 } ?: kept.size
        return Result.success(
            current.copy(entries = kept.take(at) + declared + kept.drop(at))
        )
    }

    /**
     * Whether an entry declares a bound, including one the file currently holds as invalid.
     *
     * [InvalidLineReason.EqualSizeBounds] is the only reason that describes a line's neighbours
     * rather than its own text: the line is a well-formed bound that collided with the bounds
     * before it. Taking those away makes it a live bound again, so it has to go with them. Every
     * other reason stays invalid whatever surrounds it, so those lines are left alone.
     */
    private fun isBoundLine(entry: MegaIgnoreEntry): Boolean =
        entry is MegaIgnoreEntry.SizeSmallerEntry ||
                entry is MegaIgnoreEntry.SizeLargerEntry ||
                (entry is MegaIgnoreEntry.InvalidEntry &&
                        entry.reason == InvalidLineReason.EqualSizeBounds)
}
