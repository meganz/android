package mega.privacy.android.domain.usecase.node.clouddrive

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.search.SensitivityFilterOption
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
class HasMediaChildrenUseCaseTest {

    private lateinit var underTest: HasMediaChildrenUseCase

    private val nodeRepository = mock<NodeRepository>()

    private val nodeId = NodeId(1L)

    @BeforeAll
    fun setUp() {
        underTest = HasMediaChildrenUseCase(nodeRepository = nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that invoke returns the repository answer`() = runTest {
        whenever(nodeRepository.hasMediaChildren(nodeId, null)).thenReturn(true)

        val actual = underTest(nodeId = nodeId, excludeSensitives = false)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that invoke excludes sensitive media when requested`() = runTest {
        whenever(nodeRepository.hasMediaChildren(nodeId, SensitivityFilterOption.NonSensitiveOnly))
            .thenReturn(false)

        val actual = underTest(nodeId = nodeId, excludeSensitives = true)

        assertThat(actual).isFalse()
        verify(nodeRepository).hasMediaChildren(nodeId, SensitivityFilterOption.NonSensitiveOnly)
    }
}
