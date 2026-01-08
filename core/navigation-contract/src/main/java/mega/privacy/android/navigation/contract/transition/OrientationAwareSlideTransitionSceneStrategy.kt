package mega.privacy.android.navigation.contract.transition

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay

/**
 * Scene strategy that applies orientation-aware slide transitions to navigation entries
 *
 * @param isInLandscapeMode Whether the device is currently in landscape mode
 */
class OrientationAwareSlideTransitionSceneStrategy<T : Any>(
    private val isInLandscapeMode: Boolean,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>,
    ): Scene<T>? {
        if (entries.isEmpty()) return null

        val current = entries.last()

        if (current.metadata[orientationAwareSlideTransitionKey] != true) return null

        return OrientationAwareSlideTransitionScene(
            currentEntry = current,
            previousEntries = entries.dropLast(1),
            isInLandscapeMode = isInLandscapeMode,
        )
    }
}


private class OrientationAwareSlideTransitionScene<T : Any>(
    val currentEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    isInLandscapeMode: Boolean,
) : Scene<T> {
    override val key = currentEntry.contentKey
    override val entries: List<NavEntry<T>> = listOf(currentEntry)
    override val content: @Composable (() -> Unit) = { currentEntry.Content() }

    override val metadata: Map<String, Any> = currentEntry.metadata +
            if (!isInLandscapeMode) {
                NavDisplay.transitionSpec { slideForwardTransition } +
                        NavDisplay.popTransitionSpec { slideBackwardTransition } +
                        NavDisplay.predictivePopTransitionSpec { slideBackwardTransition }
            } else {
                NavDisplay.transitionSpec { fadeTransition } +
                        NavDisplay.popTransitionSpec { fadeTransition } +
                        NavDisplay.predictivePopTransitionSpec { fadeTransition }
            }
}

/**
 * Metadata to enable orientation-aware slide transition for a navigation entry.
 */
val orientationAwareSlideTransition = mapOf(orientationAwareSlideTransitionKey to true)

private const val orientationAwareSlideTransitionKey = "orientationAwareSlideTransitionKey"
