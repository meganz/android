package mega.privacy.android.domain.usecase.node.namecollision

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateNodeNameCollisionsResultUseCaseTest {
    private lateinit var underTest: UpdateNodeNameCollisionsResultUseCase

    private val getNodeNameCollisionRenameNameUseCase =
        mock<GetNodeNameCollisionRenameNameUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateNodeNameCollisionsResultUseCase(
            getNodeNameCollisionRenameNameUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeNameCollisionRenameNameUseCase
        )
    }

    @Test
    fun `test that no collisions is returned when empty list is provided`() = runTest {
        val result = underTest(
            emptyList(),
            emptyList(),
            false
        )

        assertThat(result.first).isEmpty()
        assertThat(result.second).isEmpty()
    }

    @Test
    fun `test that non-file collisions are ignored`() = runTest {
        val nodeNameCollision = NodeNameCollisionResult(
            nameCollision = NodeNameCollision.Default(
                collisionHandle = 1L,
                nodeHandle = 2L,
                name = "test",
                size = 100L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 0L,
                parentHandle = 0L,
                isFile = false,
                type = NodeNameCollisionType.COPY,
                renameName = null
            ),
        )
        val result = underTest(
            listOf(nodeNameCollision),
            emptyList(),
            false
        )

        assertThat(result.first).isEqualTo(listOf(nodeNameCollision))
        assertThat(result.second).isEmpty()
    }

    @Test
    fun `test that collisions rename names are updated when expected rename name already exists`() =
        runTest {
            val nodeNameCollision = NodeNameCollisionResult(
                nameCollision = NodeNameCollision.Default(
                    collisionHandle = 1L,
                    nodeHandle = 2L,
                    name = "test",
                    size = 100L,
                    childFolderCount = 0,
                    childFileCount = 0,
                    lastModified = 0L,
                    parentHandle = 0L,
                    isFile = true,
                    type = NodeNameCollisionType.COPY,
                    renameName = "test (1)"
                ),
            )
            whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("test (1)")
            val result = underTest(
                listOf(nodeNameCollision),
                listOf("test (1)"),
                false
            )

            assertThat(result.first.first().nameCollision.renameName).isEqualTo("test (2)")
            assertThat(result.second).isEqualTo(listOf("test (1)"))
        }

    @Test
    fun `test that new rename name is added to the list when applyOnNext is true`() = runTest {
        val nodeNameCollision = NodeNameCollisionResult(
            nameCollision = NodeNameCollision.Default(
                collisionHandle = 1L,
                nodeHandle = 2L,
                name = "test",
                size = 100L,
                childFolderCount = 0,
                childFileCount = 0,
                lastModified = 0L,
                parentHandle = 0L,
                isFile = true,
                type = NodeNameCollisionType.COPY,
                renameName = "test (1)"
            ),
        )
        whenever(getNodeNameCollisionRenameNameUseCase(any())).thenReturn("test (1)")
        val result = underTest(
            listOf(nodeNameCollision),
            listOf("test (1)"),
            true
        )

        assertThat(result.first.first().nameCollision.renameName).isEqualTo("test (2)")
        assertThat(result.second).contains("test (2)")
    }
}