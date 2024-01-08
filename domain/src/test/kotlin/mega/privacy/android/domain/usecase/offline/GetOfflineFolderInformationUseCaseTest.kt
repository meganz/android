package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [GetOfflineFolderInformationUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflineFolderInformationUseCaseTest {
    private lateinit var underTest: GetOfflineFolderInformationUseCase

    private val nodeRepository = mock<NodeRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineFolderInformationUseCase(
            nodeRepository = nodeRepository,
        )
    }

    @ParameterizedTest
    @MethodSource("provideParameters")
    internal fun `test that OfflineFolderInfo is returned`(
        parentId: Int,
        repositoryResult: OfflineFolderInfo?,
        expected: OfflineFolderInfo,
    ) = runTest {
        whenever(nodeRepository.getOfflineFolderInfo(parentId)).thenReturn(repositoryResult)
        val actual = underTest(parentId)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        // Test case 1: Repository returns OfflineFolderInfo
        Arguments.of(1, OfflineFolderInfo(0, 1), OfflineFolderInfo(0, 1)),

        // Test case 2: Repository returns null
        Arguments.of(1, null, OfflineFolderInfo(0, 0))
    )

}