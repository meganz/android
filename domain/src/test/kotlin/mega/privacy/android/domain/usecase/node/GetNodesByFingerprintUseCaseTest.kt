package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetNodesByFingerprintUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetNodesByFingerprintUseCaseTest {

    private lateinit var underTest: GetNodesByFingerprintUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetNodesByFingerprintUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that the nodes with the same fingerprint are returned`() = runTest {
        val fingerprint = "fingerprint"
        val nodeList = listOf(mock<FileNode>(), mock<FileNode>())

        whenever(nodeRepository.getNodesByFingerprint(fingerprint)).thenReturn(nodeList)

        assertThat(underTest(fingerprint)).isEqualTo(nodeList)
    }
}