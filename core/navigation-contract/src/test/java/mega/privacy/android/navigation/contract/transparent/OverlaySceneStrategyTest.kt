package mega.privacy.android.navigation.contract.transparent

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Covers both strategies that draw an entry over the entries below it. They run the same logic
 * and differ only in the metadata that marks an entry for them, so each case runs against both.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverlaySceneStrategyTest {

    private fun strategies(): Stream<Arguments> = Stream.of(
        Arguments.of(TransparentSceneStrategy<NavKey>(), transparentMetadata()),
        Arguments.of(OverContentSceneStrategy<NavKey>(), buildMetadata { withOverContent() }),
    )

    private fun allStrategies(): Stream<Arguments> = Stream.of(
        Arguments.of(TransparentSceneStrategy<NavKey>()),
        Arguments.of(OverContentSceneStrategy<NavKey>()),
    )

    @ParameterizedTest
    @MethodSource("allStrategies")
    fun `test that no scene is returned when no entry is marked`(
        underTest: SceneStrategy<NavKey>,
    ) {
        val entries = listOf(entry("below", emptyMap()), entry("top", emptyMap()))

        assertThat(underTest.sceneFor(entries)).isNull()
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `test that no scene is returned when a marked entry is not the last entry`(
        underTest: SceneStrategy<NavKey>,
        marked: Map<String, Any>,
    ) {
        val entries = listOf(entry("below", marked), entry("top", emptyMap()))

        assertThat(underTest.sceneFor(entries)).isNull()
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `test that no scene is returned when the marked entry is the only entry`(
        underTest: SceneStrategy<NavKey>,
        marked: Map<String, Any>,
    ) {
        val entries = listOf(entry("top", marked))

        assertThat(underTest.sceneFor(entries)).isNull()
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `test that the marked entry is the only entry of the scene`(
        underTest: SceneStrategy<NavKey>,
        marked: Map<String, Any>,
    ) {
        val top = entry("top", marked)
        val entries = listOf(entry("below", emptyMap()), top)

        assertThat(underTest.sceneFor(entries)?.entries).containsExactly(top)
    }

    @ParameterizedTest
    @MethodSource("strategies")
    fun `test that the entries below the marked entry are overlaid`(
        underTest: SceneStrategy<NavKey>,
        marked: Map<String, Any>,
    ) {
        val below = entry("below", emptyMap())
        val entries = listOf(below, entry("top", marked))

        val scene = underTest.sceneFor(entries) as OverlayScene<NavKey>

        assertThat(scene.overlaidEntries).containsExactly(below)
        assertThat(scene.previousEntries).containsExactly(below)
    }

    @Test
    fun `test that the transparent strategy returns no scene when the entry is marked as over content`() {
        val underTest = TransparentSceneStrategy<NavKey>()
        val entries = listOf(entry("below", emptyMap()), entry("top", buildMetadata { withOverContent() }))

        assertThat(underTest.sceneFor(entries)).isNull()
    }

    @Test
    fun `test that the over content strategy returns no scene when the entry is marked as transparent`() {
        val underTest = OverContentSceneStrategy<NavKey>()
        val entries = listOf(entry("below", emptyMap()), entry("top", transparentMetadata()))

        assertThat(underTest.sceneFor(entries)).isNull()
    }

    @Serializable
    private data class TestNavKey(val id: String) : NavKey

    private fun entry(id: String, metadata: Map<String, Any>) = NavEntry<NavKey>(
        key = TestNavKey(id),
        metadata = metadata,
        content = { },
    )

    private fun SceneStrategy<NavKey>.sceneFor(entries: List<NavEntry<NavKey>>) =
        with(this) { SceneStrategyScope<NavKey>().calculateScene(entries) }
}
