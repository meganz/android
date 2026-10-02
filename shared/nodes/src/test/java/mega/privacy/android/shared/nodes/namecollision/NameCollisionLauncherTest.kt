package mega.privacy.android.shared.nodes.namecollision

import androidx.navigation3.runtime.NavKey
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NameCollisionLauncherTest {

    @Test
    fun `test that launch parks the collisions in the holder under the id it navigates with`() {
        val holder = NameCollisionLaunchSourceHolder()
        val navigated = mutableListOf<NavKey>()
        val collisions = listOf(mock<NodeNameCollision.Default>(), mock<NodeNameCollision.Default>())
        val underTest = NameCollisionLauncher(holder, navigated::add)

        underTest.launch(collisions)

        val key = navigated.single() as NameCollisionNavKey
        assertThat(holder.consume(key.launchId)).isEqualTo(
            NameCollisionArgs.Multiple(collisions = collisions, isFolderUploadContext = false)
        )
    }

    @Test
    fun `test that launch uses a fresh id every time`() {
        val holder = NameCollisionLaunchSourceHolder()
        val navigated = mutableListOf<NavKey>()
        val underTest = NameCollisionLauncher(holder, navigated::add)

        underTest.launch(listOf(mock<NodeNameCollision.Default>()))
        underTest.launch(listOf(mock<NodeNameCollision.Default>()))

        val ids = navigated.map { (it as NameCollisionNavKey).launchId }
        assertThat(ids.toSet()).hasSize(2)
        ids.forEach { assertThat(holder.consume(it)).isNotNull() }
    }
}
