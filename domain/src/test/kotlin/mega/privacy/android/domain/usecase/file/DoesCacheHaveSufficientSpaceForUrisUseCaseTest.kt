package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesCacheHaveSufficientSpaceForUrisUseCaseTest {
    private lateinit var underTest: DoesCacheHaveSufficientSpaceForUrisUseCase

    private val doesPathHaveSufficientSpaceUseCase = mock<DoesPathHaveSufficientSpaceUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val cacheRepository = mock<CacheRepository>()

    @BeforeAll
    fun setup() {
        underTest = DoesCacheHaveSufficientSpaceForUrisUseCase(
            doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository,
            cacheRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        doesPathHaveSufficientSpaceUseCase,
        fileSystemRepository,
        cacheRepository,
    )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that doesPathHaveSufficientSpaceUseCase is called with the correct parameters and its result is returned`(
        expected: Boolean,
    ) = runTest {
        val destination = File("file:/somewhere")
        val urisToSize = mapOf(
            "uri://example1" to 342L,
            "uri://example2" to 7645L,
            "uri://example3" to 984L,
            "uri://example4" to 8974L,
        )
        val totalSpace = urisToSize.values.sum()
        urisToSize.forEach { (uriString, uriSize) ->
            whenever(fileSystemRepository.getFileSizeFromUri(uriString)) doReturn uriSize
        }
        whenever(cacheRepository.getCacheFolder(any())) doReturn destination
        whenever(doesPathHaveSufficientSpaceUseCase(destination.path, totalSpace)) doReturn expected

        val actual = underTest(urisToSize.keys.toList())

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that false is returned when the cache folder is not found`() = runTest {
        val urisToSize = mapOf(
            "uri://example1" to 342L,
            "uri://example2" to 7645L,
            "uri://example3" to 984L,
            "uri://example4" to 8974L,
        )
        whenever(cacheRepository.getCacheFolder(any())) doReturn null

        val actual = underTest(urisToSize.keys.toList())

        assertThat(actual).isEqualTo(false)
        verifyNoInteractions(doesPathHaveSufficientSpaceUseCase, fileSystemRepository)
    }
}