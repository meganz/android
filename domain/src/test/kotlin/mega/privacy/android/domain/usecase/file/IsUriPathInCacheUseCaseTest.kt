package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsUriPathInCacheUseCaseTest {
    private lateinit var underTest: IsUriPathInCacheUseCase
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val cacheRepository = mock<CacheRepository>()


    @BeforeAll
    fun setup() {
        underTest = IsUriPathInCacheUseCase(
            fileSystemRepository,
            cacheRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            fileSystemRepository,
            cacheRepository,
        )
    }

    @Test
    fun `test that use case returns false when get path from repository is null`() = runTest {
        val uri = UriPath("foo")
        whenever(fileSystemRepository.getAbsolutePathByContentUri(uri.value)) doReturn null

        val actual = underTest(uri)

        assertThat(actual).isFalse()
        verifyNoInteractions(cacheRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case gets the result from cache repository when get path from repository is not null`(
        expected: Boolean,
    ) = runTest {
        val uri = UriPath("foo")
        val path = "boo"
        val file = File(path)
        whenever(fileSystemRepository.getAbsolutePathByContentUri(uri.value)) doReturn path
        whenever(cacheRepository.isFileInCacheDirectory(file)) doReturn expected

        val actual = underTest(uri)

        assertThat(actual).isEqualTo(expected)
    }
}