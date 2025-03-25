package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanReadUriUseCaseTest {

    private lateinit var underTest: CanReadUriUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CanReadUriUseCase(fileSystemRepository)
    }

    @BeforeAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest(name = "pauseTransfers: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that use case returns correctly`(canRead: Boolean) = runTest {
        val uri = "content://com.android.externalstorage.documents/tree/"

        whenever(fileSystemRepository.canReadUri(uri)).thenReturn(canRead)

        assertThat(underTest(uri)).isEqualTo(canRead)
    }
}