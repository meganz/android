package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetLastModifiedTimeUseCaseTest {

    private lateinit var underTest: GetLastModifiedTimeUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetLastModifiedTimeUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that use case invokes and returns correctly`() = runTest {
        val uriPath = UriPath("filePath")
        val lastModifiedTime = Instant.fromEpochMilliseconds(123456789L)

        whenever(fileSystemRepository.getLastModifiedTime(uriPath)) doReturn lastModifiedTime

        assertThat(underTest(uriPath)).isEqualTo(lastModifiedTime)
    }
}