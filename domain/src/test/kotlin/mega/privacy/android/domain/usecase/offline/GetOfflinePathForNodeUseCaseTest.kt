package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetOfflinePathForNodeUseCaseTest {
    private lateinit var underTest: GetOfflinePathForNodeUseCase

    private val getOfflineFileUseCase = mock<GetOfflineFileUseCase>()
    private val getOfflineNodeInformationUseCase = mock<GetOfflineNodeInformationUseCase>()


    @BeforeAll
    fun setup() {
        underTest = GetOfflinePathForNodeUseCase(
            getOfflineFileUseCase = getOfflineFileUseCase,
            getOfflineNodeInformationUseCase = getOfflineNodeInformationUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getOfflineFileUseCase,
            getOfflineNodeInformationUseCase,
        )
    }

    @Test
    fun `test that correct path is returned when use case is invoked`() =
        runTest {
            val node = mock<Node>()
            val folderPath = "/offline/destination"
            val expected = folderPath.plus(File.separator)
            val folder = mock<File> {
                on { path }.thenReturn(folderPath)
            }
            val file = mock<File> {
                on { parentFile }.thenReturn(folder)
            }
            val info = mock<OtherOfflineNodeInformation>()
            whenever(getOfflineNodeInformationUseCase(node)).thenReturn(info)
            whenever(getOfflineFileUseCase(info)).thenReturn(file)
            val actual = underTest(node)
            assertThat(actual).isEqualTo(expected)
        }
}