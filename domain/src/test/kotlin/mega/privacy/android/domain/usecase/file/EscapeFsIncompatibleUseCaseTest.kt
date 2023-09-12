package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EscapeFsIncompatibleUseCaseTest {

    private lateinit var underTest: EscapeFsIncompatibleUseCase

    private lateinit var fileSystemRepository: FileSystemRepository

    @BeforeAll
    fun setup() {
        fileSystemRepository = mock()
        underTest = EscapeFsIncompatibleUseCase(fileSystemRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @ParameterizedTest(name = " repository call returns {0}")
    @NullAndEmptySource
    @ValueSource(strings = ["testName"])
    fun `test that EscapeFsIncompatibleUseCase returns correctly if`(
        fileName: String?,
    ) = runTest {
        whenever(fileSystemRepository.escapeFsIncompatible(any(), any())).thenReturn(fileName)
        Truth.assertThat(underTest("file name", "dest/path")).isEqualTo(fileName)
    }
}