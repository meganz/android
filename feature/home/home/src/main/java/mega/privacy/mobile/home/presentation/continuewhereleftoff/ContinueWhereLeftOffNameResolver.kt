package mega.privacy.mobile.home.presentation.continuewhereleftoff

import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.domain.entity.toDuration
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.continuewhereleftoff.RemoveRecentlyUsedItemUseCase
import javax.inject.Inject

/**
 * Resolves blank titles and audio/video durations in [ContinueWhereLeftOffItem] lists
 * by fetching node data from [GetNodeByIdUseCase]. Results are cached by nodeHandle
 * so each node is resolved at most once per instance lifetime.
 */
internal class ContinueWhereLeftOffNameResolver @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
    private val removeRecentlyUsedItemUseCase: RemoveRecentlyUsedItemUseCase,
) {
    private data class ResolvedData(val name: String, val duration: String?)

    private val cache = mutableMapOf<Long, ResolvedData>()

    fun applyCachedNames(items: List<ContinueWhereLeftOffItem>) = items.map { item ->
        cache[item.nodeHandle]?.let { resolved ->
            item.copy(
                title = resolved.name.ifBlank { item.title },
                duration = resolved.duration ?: item.duration,
            )
        } ?: item
    }

    suspend fun resolveBlankNames(items: List<ContinueWhereLeftOffItem>): Boolean {
        val unresolved = items.filter { it.nodeHandle !in cache && (it.title.isBlank() || it.duration == null) }
        if (unresolved.isEmpty()) return false
        val sizeBefore = cache.size
        unresolved.forEach { item ->
            val node = runCatching {
                getNodeByIdUseCase(NodeId(item.nodeHandle))
            }.getOrNull()
            if (node != null) {
                val duration = (node as? TypedFileNode)?.type?.toDuration()
                cache[item.nodeHandle] = ResolvedData(
                    name = node.name,
                    duration = duration?.let { durationInSecondsTextMapper(it) },
                )
            } else {
                runCatching { removeRecentlyUsedItemUseCase(item.nodeHandle) }
            }
        }
        return cache.size > sizeBefore
    }
}
