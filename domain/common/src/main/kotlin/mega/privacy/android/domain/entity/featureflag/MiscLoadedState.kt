package mega.privacy.android.domain.entity.featureflag

/**
 * Sealed class representing the state of misc flags loading
 */
sealed interface MiscLoadedState {
    /**
     * Indicates that getUserData() has not been called yet
     */
    object NotLoaded : MiscLoadedState

    /**
     * Indicates that getUserData() method has been called
     */
    object MethodCalled : MiscLoadedState

    /**
     * Indicates that misc flags are ready
     */
    object FlagsReady : MiscLoadedState
}