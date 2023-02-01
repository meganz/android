package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

internal class DefaultDoesPathHaveSufficientSpaceTest {
    private lateinit var underTest: DoesPathHaveSufficientSpace

    private val fileSystemRepository = mock<FileSystemRepository>()

    @Before
    fun setUp() {
        underTest = DefaultDoesPathHaveSufficientSpace(
            fileSystemRepository = fileSystemRepository
        )
    }

    @Test
    fun `test that if the path has less space than required, false is returned`() = runTest {
        val required = 123L
        fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenReturn(required - 1) }

        assertThat(underTest("", required)).isFalse()
    }

    @Test
    fun `test that if the path has more space than required, true is returned`() = runTest {
        val required = 123L
        fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenReturn(required + 1) }

        assertThat(underTest("", required)).isTrue()
    }

    @Test
    fun `test that if file repository throws an error, true is returned`() = runTest {
        val required = 123L
        fileSystemRepository.stub { onBlocking { getDiskSpaceBytes(any()) }.thenAnswer { throw IllegalArgumentException() } }

        assertThat(underTest("", required)).isTrue()
    }
}