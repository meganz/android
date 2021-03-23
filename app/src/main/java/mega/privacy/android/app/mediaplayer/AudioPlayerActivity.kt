package mega.privacy.android.app.mediaplayer

/**
 * Extending MediaPlayerActivity is to declare portrait in manifest,
 * to avoid crash when set requestedOrientation.
 */
class AudioPlayerActivity : MediaPlayerActivity() {
    override fun isAudioPlayer() = true
}
