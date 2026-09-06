package mega.privacy.android.feature.mediaplayer.data

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process mapping table that associates opaque UUID media IDs with raw MEGA node handles.
 *
 * [mega.privacy.android.feature.mediaplayer.data.mapper.AudioNodeToMediaItemMapper] generates a
 * random UUID for each [androidx.media3.common.MediaItem] so that external observers (apps with
 * Notification Listener permission) see only random strings instead of raw node handles.
 * This store is the authoritative lookup used by service and gateway code that needs the real
 * handle back from a media ID.
 */
@Singleton
class MediaHandleStore @Inject constructor() {
    private val store = ConcurrentHashMap<String, Long>()

    fun register(mediaId: String, handle: Long) {
        store[mediaId] = handle
    }

    fun getHandle(mediaId: String): Long? = store[mediaId]

    /**
     * Removes the mappings for [mediaIds] only. Scoped removal keeps entries registered by a
     * queue build that is still in flight intact, unlike [clear].
     */
    fun remove(mediaIds: Collection<String>) {
        mediaIds.forEach { store.remove(it) }
    }

    fun clear() {
        store.clear()
    }
}
