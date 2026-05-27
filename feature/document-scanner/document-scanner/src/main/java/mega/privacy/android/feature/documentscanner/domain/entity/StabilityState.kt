package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * Stability state of the detected document boundary across consecutive frames.
 */
enum class StabilityState {
    /** No document detected in the current frame */
    SEARCHING,

    /** Document detected but moving or jittering */
    UNSTABLE,

    /** Document boundary is converging — held mostly still for a few frames */
    STABILIZING,

    /** Document boundary is stable across enough consecutive frames for capture */
    STABLE,
}
