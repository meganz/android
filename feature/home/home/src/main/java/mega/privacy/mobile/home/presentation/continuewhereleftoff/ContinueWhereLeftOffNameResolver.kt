package mega.privacy.mobile.home.presentation.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import javax.inject.Inject

/**
 * Resolves blank titles in [ContinueWhereLeftOffItem] lists by fetching node
 * names from [GetNodeByIdUseCase]. Results are cached by nodeHandle so each
 * node is resolved at most once per instance lifetime.
 */
internal class ContinueWhereLeftOffNameResolver @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) {
    private val cache = mutableMapOf<Long, String>()

    fun applyCachedNames(items: List<ContinueWhereLeftOffItem>) = items.map { item ->
        cache[item.nodeHandle]?.let { item.copy(title = it) } ?: item
    }

    suspend fun resolveBlankNames(items: List<ContinueWhereLeftOffItem>): Boolean {
        val blanks = items.filter { it.title.isBlank() && it.nodeHandle !in cache }
        if (blanks.isEmpty()) return false
        val sizeBefore = cache.size
        blanks.forEach { item ->
            val node = runCatching {
                getNodeByIdUseCase(NodeId(item.nodeHandle))
            }.getOrNull()
            if (node != null) {
                cache[item.nodeHandle] = node.name
            }
        }
        return cache.size > sizeBefore
    }
}
