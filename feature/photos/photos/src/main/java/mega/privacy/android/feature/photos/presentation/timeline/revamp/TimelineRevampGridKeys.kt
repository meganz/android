package mega.privacy.android.feature.photos.presentation.timeline.revamp

import mega.privacy.android.feature.photos.presentation.timeline.TimelineDateCache

/**
 * Encoding and decoding of the LazyGrid item keys used by the Timeline Revamp grid.
 *
 * Media keys are intentionally independent of the global slot index, so media added or removed
 * elsewhere doesn't reshuffle keys and the grid keeps its scroll anchor; [globalMediaIndexOf]
 * reconstructs the global index on demand from the current per-section offsets.
 */

internal const val ENABLE_CU_BANNER = "timeline_revamp_content:banner"
internal const val NON_STICKY_HEADER_ITEM = "timeline_revamp_content:non_sticky_header"

internal const val HEADER_KEY_PREFIX = "header_"

private const val MEDIA_KEY_PREFIX = "media_"

/**
 * Separator in a media item key `media_<localIndex>@<groupId>`. The key is independent of the global
 * index, so media added/removed elsewhere doesn't reshuffle keys and the grid keeps its scroll anchor.
 */
private const val MEDIA_KEY_SEPARATOR = "@"

/** Stable `<year>-<month>` key/tag suffix for the month a section's start date falls in. */
internal fun monthKey(startDateSeconds: Long): String {
    val date = TimelineDateCache.get(startDateSeconds)
    return "${date.year}-${date.monthValue}"
}

internal fun mediaKey(groupId: String, localIndex: Int): String =
    "$MEDIA_KEY_PREFIX$localIndex$MEDIA_KEY_SEPARATOR$groupId"

/**
 * Recovers `(groupId, localIndex)` from a media item key, or null if [key] is not a media key.
 */
private fun parseMediaKey(key: Any?): Pair<String, Int>? {
    val raw = (key as? String)?.takeIf { it.startsWith(MEDIA_KEY_PREFIX) }
        ?.removePrefix(MEDIA_KEY_PREFIX)
        ?: return null
    val separator = raw.indexOf(MEDIA_KEY_SEPARATOR)
    if (separator <= 0) return null
    val localIndex = raw.substring(0, separator).toIntOrNull() ?: return null
    return raw.substring(separator + 1) to localIndex
}

/**
 * Resolves a media item key to its global index using [offsetByGroupId] (the current section start
 * offsets keyed by `groupId`), or null when [key] is not a media key / its section is gone.
 */
internal fun globalMediaIndexOf(key: Any?, offsetByGroupId: Map<String, Int>): Int? {
    val (groupId, localIndex) = parseMediaKey(key) ?: return null
    val base = offsetByGroupId[groupId] ?: return null
    return base + localIndex
}
