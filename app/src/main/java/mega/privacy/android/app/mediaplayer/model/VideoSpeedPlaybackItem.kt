package mega.privacy.android.app.mediaplayer.model

/**
 * The enum class for video playback speed
 *
 * @property speed playback speed
 * @property text speed tex
 */
enum class VideoSpeedPlaybackItem(
    override val speed: Float,
    override val text: String,
) : SpeedPlaybackItem {
    /**
     * 0.5x playback speed
     */
    PlaybackSpeed_0_5X(speed = 0.5F, text = "0.5x"),

    /**
     * 1x playback speed
     */
    PlaybackSpeed_1X(speed = 1F, text = "1x"),

    /**
     * 1.5x playback speed
     */
    PlaybackSpeed_1_5X(speed = 1.5F, text = "1.5x"),


    /**
     * 2x playback speed
     */
    PlaybackSpeed_2X(speed = 2F, text = "2x"),
}
