package mega.privacy.android.shared.nodes.namecollision

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-local handoff for [NameCollisionArgs] payloads that are too rich to carry inside a
 * navigation key: the collisions hold serialized node data and file paths that would risk a
 * `TransactionTooLargeException` on back-stack save.
 *
 * The caller [put]s a source keyed by a short id (carried on
 * [mega.privacy.android.navigation.destination.NameCollisionNavKey]) BEFORE emitting the nav key,
 * and the route entry [consume]s it by that id to build the [NameCollisionViewModel].
 *
 * The holder is in-memory only: on process death it is empty, so the entry falls back to navigating
 * back when nothing is stored for the requested id.
 */
@Singleton
class NameCollisionLaunchSourceHolder @Inject constructor() {
    private val sources = ConcurrentHashMap<String, NameCollisionArgs>()

    /**
     * Store [source] under [id]. Overwrites any existing entry for the same id.
     */
    fun put(id: String, source: NameCollisionArgs) {
        sources[id] = source
    }

    /**
     * Remove and return the source for [id], or null if none was stored (e.g. after process death).
     */
    fun consume(id: String): NameCollisionArgs? = sources.remove(id)
}
