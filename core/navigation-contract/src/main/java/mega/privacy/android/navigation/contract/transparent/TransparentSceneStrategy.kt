package mega.privacy.android.navigation.contract.transparent

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Scene strategy that creates transparent scenes for navigation entries marked with
 * transparent metadata. This prevents UI flashing when entries just launch legacy activities
 * and immediately remove themselves.
 */
class TransparentSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.isEmpty()) return null

        val current = entries.last()
        if (current.isTransparent().not()) return null

        return TransparentScene(
            key = current.contentKey,
            transparentEntry = current,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
        )
    }
}

