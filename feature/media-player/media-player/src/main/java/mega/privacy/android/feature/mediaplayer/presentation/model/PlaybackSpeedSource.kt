package mega.privacy.android.feature.mediaplayer.presentation.model

/** Which control in the playback speed sheet produced a speed change. */
enum class PlaybackSpeedSource {
    /** One of the preset buttons (1×, 1.25×, 1.5×, 1.75×, 2×). */
    Preset,

    /** The slider, released at any tick. */
    Slider,
}
