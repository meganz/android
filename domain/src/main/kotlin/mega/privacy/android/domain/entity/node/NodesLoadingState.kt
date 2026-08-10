package mega.privacy.android.domain.entity.node

/**
 * Sealed interface representing the different states of progressive node loading
 */
sealed interface NodesLoadingState {
    object Idle : NodesLoadingState
    object Loading : NodesLoadingState
    object PartiallyLoaded : NodesLoadingState
    object FullyLoaded : NodesLoadingState
    object Failed : NodesLoadingState

    val isInProgress: Boolean
        get() = this == Loading || this == PartiallyLoaded

    val isComplete: Boolean
        get() = this == FullyLoaded || this == Failed
}