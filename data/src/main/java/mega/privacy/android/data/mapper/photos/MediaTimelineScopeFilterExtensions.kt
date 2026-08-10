package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import nz.mega.sdk.MegaNodeScopeFilter

/**
 * Applies the location scope of [filter] to this SDK [MegaNodeScopeFilter], shared by
 * [MediaTimelineFilterMapper] and [MediaTimelineListFilterMapper] (both produce subtypes of it).
 *
 * - [MediaTimelineFilter.includeLocationHandles] non-empty → restrict to those folder handles
 *   (e.g. Camera Upload + Media Upload only), taking precedence over the location scope.
 * - [MediaTimelineFilter.excludeLocationHandles] non-empty → apply the [MediaTimelineFilter.location]
 *   scope and exclude those folder handles (e.g. Cloud Drive excluding Camera Upload).
 * - otherwise → apply the [MediaTimelineFilter.location] scope.
 */
internal fun MegaNodeScopeFilter.applyMediaTimelineLocation(
    filter: MediaTimelineFilter,
    locationIntMapper: MediaTimelineLocationIntMapper,
    megaHandleListMapper: MegaHandleListMapper,
) {
    when {
        filter.includeLocationHandles.isNotEmpty() ->
            megaHandleListMapper(filter.includeLocationHandles.map { it.longValue })
                ?.let { byLocationHandles(it) }

        filter.excludeLocationHandles.isNotEmpty() -> {
            byLocation(locationIntMapper(filter.location))
            megaHandleListMapper(filter.excludeLocationHandles.map { it.longValue })
                ?.let { byExcludeLocationHandles(it) }
        }

        else -> byLocation(locationIntMapper(filter.location))
    }
}
