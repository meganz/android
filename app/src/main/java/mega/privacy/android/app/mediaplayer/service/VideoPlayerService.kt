package mega.privacy.android.app.mediaplayer.service

/**
 * Extending MediaPlayerService is to support two running instances at the same time,
 * video player and audio player, so that video player could "interrupt" audio player,
 * and resume audio player when video player is stopped.
 */
class VideoPlayerService : MediaPlayerService()
