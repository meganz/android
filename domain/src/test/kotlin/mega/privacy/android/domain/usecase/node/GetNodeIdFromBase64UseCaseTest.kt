package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeIdFromBase64UseCaseTest {
    private lateinit var underTest: GetNodeIdFromBase64UseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = GetNodeIdFromBase64UseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that use case returns NodeId when base64 converts to valid handle`() = runTest {
        val base64Node = "86YkxIDC"
        val validHandle = 123456789L
        val invalidHandle = -1L

        whenever(nodeRepository.convertBase64ToHandle(base64Node)).thenReturn(validHandle)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(invalidHandle)

        val result = underTest(base64Node)

        assertThat(result).isEqualTo(NodeId(validHandle))
    }

    @Test
    fun `test that use case returns null when base64 converts to invalid handle`() = runTest {
        val base64Node = "invalidBase64"
        val invalidHandle = -1L

        whenever(nodeRepository.convertBase64ToHandle(base64Node)).thenReturn(invalidHandle)
        whenever(nodeRepository.getInvalidHandle()).thenReturn(invalidHandle)

        val result = underTest(base64Node)

        assertThat(result).isNull()
    }
}

