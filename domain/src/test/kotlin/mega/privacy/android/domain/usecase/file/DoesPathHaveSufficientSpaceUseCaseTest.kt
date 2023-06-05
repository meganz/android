package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub


@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DoesPathHaveSufficientSpaceUseCaseTest {
    private lateinit var underTest: DoesPathHaveSufficientSpaceUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DoesPathHaveSufficientSpaceUseCase(
            fileSystemRepository = fileSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    internal fun `test that false is returned if the path has less space than required, `() =
        runTest {
            val required = 123L
            fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenReturn(required - 1) }

            assertThat(underTest("", required)).isFalse()
        }

    @Test
    internal fun `test that true is returned if the path has more space than required`() =
        runTest {
            val required = 123L
            fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenReturn(required + 1) }

            assertThat(underTest("", required)).isTrue()
        }

    @Test
    internal fun `test that false is returned if file repository throws an error`() = runTest {
        val required = 123L
        fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenAnswer { throw IllegalArgumentException() } }

        assertThat(underTest("", required)).isFalse()
    }
}