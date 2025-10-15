package mega.privacy.android.navigation.contract.bottomsheet

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.size < 2) return null

        val base = entries[entries.lastIndex - 1]
        val sheet = entries.last()
        if (sheet.isBottomSheet().not()) return null
        val skipPartiallyExpanded = sheet.skipPartiallyExpanded()

        val key = Pair(base.contentKey, sheet.contentKey)
        val previous = entries.dropLast(1)
        return BottomSheetScene(
            key = key,
            previousEntries = previous,
            overlaidEntries = previous,
            sheetEntry = sheet,
            onBack = onBack,
            skipPartiallyExpanded = skipPartiallyExpanded,
        )
    }
}