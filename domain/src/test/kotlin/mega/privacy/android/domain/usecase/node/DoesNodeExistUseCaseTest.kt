package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesNodeExistUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val underTest = DoesNodeExistUseCase(nodeRepository)

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository
        )
    }

    @ParameterizedTest(name = "if node exists {0}e")
    @ValueSource(booleans = [true, false])
    fun `test that use case returns correct value`(
        exists: Boolean,
    ) = runTest {
        val nodeId = NodeId(1)
        whenever(nodeRepository.doesNodeExist(nodeId)).thenReturn(exists)
        assertThat(underTest(nodeId)).isEqualTo(exists)
    }
}