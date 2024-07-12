package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetOfflineFilesUseCaseTest {

    private lateinit var underTest: GetOfflineFilesUseCase

    private val getOfflineFileUseCase: GetOfflineFileUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = GetOfflineFilesUseCase(getOfflineFileUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getOfflineFileUseCase)
    }

    @Test
    fun `test that correct files are returned when invoke is called`() = runTest {
        val offlineInformation: OfflineFileInformation = mock {
            on { id }.thenReturn(1)
        }
        val file = mock<File>()
        whenever(getOfflineFileUseCase(offlineInformation)).thenReturn(file)

        val result = underTest(listOf(offlineInformation))

        assertThat(result.values).containsExactly(file)
    }

    @Test
    fun `test that IllegalStateException is thrown when all async operation fails`() = runTest {
        val offlineInformation: OfflineFileInformation = mock {
            on { id }.thenReturn(1)
        }

        whenever(getOfflineFileUseCase(offlineInformation)).thenThrow(IllegalStateException::class.java)

        assertThrows<IllegalStateException> {
            underTest(listOf(offlineInformation))
        }
    }

    @Test
    fun `test that usecase returns files if at least one operation is successful`() = runTest {
        val offlineInformation: OfflineFileInformation = mock {
            on { id }.thenReturn(1)
        }
        val file = mock<File>()
        whenever(getOfflineFileUseCase(offlineInformation)).thenReturn(file)

        val offlineInformation2: OfflineFileInformation = mock {
            on { id }.thenReturn(2)
        }
        whenever(getOfflineFileUseCase(offlineInformation2)).thenThrow(IllegalStateException::class.java)

        val result = underTest(listOf(offlineInformation, offlineInformation2))

        assertThat(result.values).containsExactly(file)
    }

    @Test
    fun `test that empty map is returned when invoked with empty list`() = runTest {
        val result = underTest(emptyList())
        assertThat(result).isEmpty()
    }
}