package mega.privacy.android.domain.exception.node

sealed class HideNodesException() : RuntimeException() {
    /**
     * Exception thrown when Hide Nodes feature is not available for the current account type
     */
    class Unauthorized : HideNodesException()

    /**
     * Exception thrown when feature is not yet onboarded for the current account
     */
    class NotOnboarded : HideNodesException()
}