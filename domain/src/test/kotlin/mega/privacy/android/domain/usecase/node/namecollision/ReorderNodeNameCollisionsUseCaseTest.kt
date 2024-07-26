import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.usecase.node.namecollision.ReorderNodeNameCollisionsUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReorderNodeNameCollisionsUseCaseTest {

    private lateinit var underTest: ReorderNodeNameCollisionsUseCase

    @BeforeEach
    fun setUp() {
        underTest = ReorderNodeNameCollisionsUseCase()
    }

    @Test
    fun `test reordering with mixed files and folders`() = runTest {
        val collisions = listOf(
            mockNameCollision(isFile = false),
            mockNameCollision(isFile = true),
            mockNameCollision(isFile = false),
            mockNameCollision(isFile = true)
        )

        val result = underTest(collisions)

        assertThat(result.first).containsExactly(
            collisions[1], collisions[3], collisions[0], collisions[2]
        ).inOrder()
        assertThat(result.second).isEqualTo(1)
        assertThat(result.third).isEqualTo(1)
    }

    @Test
    fun `test reordering with only files`() = runTest {
        val collisions = listOf(
            mockNameCollision(isFile = true),
            mockNameCollision(isFile = true)
        )

        val result = underTest(collisions)

        assertThat(result.first).containsExactlyElementsIn(collisions).inOrder()
        assertThat(result.second).isEqualTo(1)
        assertThat(result.third).isEqualTo(0)
    }

    @Test
    fun `test reordering with only folders`() = runTest {
        val collisions = listOf(
            mockNameCollision(isFile = false),
            mockNameCollision(isFile = false)
        )

        val result = underTest(collisions)

        assertThat(result.first).containsExactlyElementsIn(collisions).inOrder()
        assertThat(result.second).isEqualTo(0)
        assertThat(result.third).isEqualTo(1)
    }

    @Test
    fun `test reordering with empty list`() = runTest {
        val collisions = emptyList<NameCollision>()

        val result = underTest(collisions)

        assertThat(result.first).isEmpty()
        assertThat(result.second).isEqualTo(0)
        assertThat(result.third).isEqualTo(0)
    }

    private fun mockNameCollision(isFile: Boolean): NameCollision {
        return NodeNameCollision.Default(
            nodeHandle = 123L,
            collisionHandle = 0,
            name = "name",
            size = 0,
            childFolderCount = 0,
            childFileCount = 0,
            lastModified = 0,
            parentHandle = 0,
            isFile = isFile
        )
    }
}