package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesUriPathExistsUseCaseTest {

    private lateinit var underTest: DoesUriPathExistsUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = DoesUriPathExistsUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun cleanUp() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that usecase returns repository value`(exists: Boolean) = runTest {
        val uriPath = UriPath("foo")
        whenever(fileSystemRepository.doesUriPathExist(uriPath)) doReturn exists

        val actual = underTest(uriPath)

        assertThat(actual).isEqualTo(exists)
    }
}