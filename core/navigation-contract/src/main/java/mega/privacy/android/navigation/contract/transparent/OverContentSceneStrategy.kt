package mega.privacy.android.navigation.contract.transparent

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Scene strategy that draws an entry marked with [withOverContent] over the entries below
 * it, which stay in the composition.
 *
 * It returns null for the first entry of the back stack, because there is nothing below it to
 * draw, so the screen must also work with nothing behind it.
 */
class OverContentSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? =
        overlaySceneOrNull(entries) { isOverContent() }
}
