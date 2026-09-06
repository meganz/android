package mega.privacy.android.feature.mediaplayer.navigation

import android.content.Intent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-local handoff for audio player launch [Intent]s.
 *
 * The launcher stores the original Intent keyed by a UUID (carried on the NavKey), and the route
 * entry consumes it to initialise the V2 player.
 *
 * Held in memory only: on process death the holder is empty, and the route entry either shows
 * the still-running session or navigates back when no session reports in.
 * Callers must invoke [consume] to avoid retaining stale Intents.
 */
@Singleton
class AudioPlayerLaunchSourceHolder @Inject constructor() {
    private val sources = ConcurrentHashMap<String, Intent>()

    fun put(id: String, intent: Intent) {
        sources[id] = intent
    }

    fun consume(id: String): Intent? = sources.remove(id)
}
