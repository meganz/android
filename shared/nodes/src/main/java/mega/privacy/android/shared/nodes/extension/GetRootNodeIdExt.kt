package mega.privacy.android.shared.nodes.extension

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import timber.log.Timber

/**
 * Resolves the cloud-drive root node id for an explorer to open at, falling back to an invalid id
 * ([NodeId] of -1) when the use case fails or returns null.
 */
suspend fun GetRootNodeIdUseCase.orInvalid(): NodeId =
    runCatching { invoke() }
        .onFailure { Timber.e(it) }
        .getOrNull() ?: NodeId(-1)
