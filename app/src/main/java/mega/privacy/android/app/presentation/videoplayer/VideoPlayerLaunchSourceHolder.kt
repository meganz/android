package mega.privacy.android.app.presentation.videoplayer

import mega.privacy.android.app.presentation.videoplayer.model.VideoPlayerLaunchSource
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-local handoff for [VideoPlayerLaunchSource] payloads that are too rich to carry inside a
 * navigation key (a content URI plus serialized node data and handle arrays would risk
 * `TransactionTooLargeException` on back-stack save).
 *
 * The launcher [put]s a source keyed by a short id (carried on
 * [mega.privacy.android.app.presentation.videoplayer.navigation.ComposeVideoPlayerScreenNavKey]),
 * and the route entry [consume]s it by that id to create the
 * [ComposeVideoPlayerViewModel].
 *
 * The launcher always [put]s the source BEFORE emitting the nav key, so the route is never reached
 * through a raw parcelable deep link with no entry here. The holder is in-memory only: on process
 * death it is empty, and the route entry falls back to the primitive subset persisted in its
 * `rememberSaveable` state.
 */
@Singleton
class VideoPlayerLaunchSourceHolder @Inject constructor() {
    private val sources = ConcurrentHashMap<String, VideoPlayerLaunchSource>()

    /**
     * Store [source] under [id]. Overwrites any existing entry for the same id.
     */
    fun put(id: String, source: VideoPlayerLaunchSource) {
        sources[id] = source
    }

    /**
     * Remove and return the source for [id], or null if none was stored (e.g. after process death).
     */
    fun consume(id: String): VideoPlayerLaunchSource? = sources.remove(id)
}
