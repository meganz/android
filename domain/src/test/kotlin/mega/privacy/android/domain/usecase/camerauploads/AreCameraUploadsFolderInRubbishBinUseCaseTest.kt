package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AreCameraUploadsFolderInRubbishBinUseCaseTest {
    lateinit var underTest: AreCameraUploadsFoldersInRubbishBinUseCase

    private val isSecondaryFolderEnabled = mock<IsSecondaryFolderEnabled>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()

    @BeforeAll
    fun setUp() {
        underTest = AreCameraUploadsFoldersInRubbishBinUseCase(
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            isNodeInRubbish = isNodeInRubbish,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isSecondaryFolderEnabled,
            isNodeInRubbish,
        )
    }

    @Test
    fun `test that when primary folder is in rubbish then the use case returns true`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(false)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(true)
            val actual = underTest(primaryHandle, secondaryHandle)
            assertThat(actual).isEqualTo(true)
        }

    @Test
    fun `test that when secondary folder is enabled and in rubbish then the use case returns true`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(true)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(true)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            val actual = underTest(primaryHandle, secondaryHandle)
            assertThat(actual).isEqualTo(true)
        }

    @Test
    fun `test that when primary folder is enabled and not in rubbish then the use case returns false`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(false)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(true)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            val actual = underTest(primaryHandle, secondaryHandle)
            assertThat(actual).isEqualTo(false)
        }

    @Test
    fun `test that when secondary folder is enabled and not in rubbish then the use case returns false`() =
        runTest {
            val primaryHandle = 12345678L
            val secondaryHandle = 87654321L
            whenever(isSecondaryFolderEnabled())
                .thenReturn(true)
            whenever(isNodeInRubbish(secondaryHandle))
                .thenReturn(false)
            whenever(isNodeInRubbish(primaryHandle))
                .thenReturn(false)
            val actual = underTest(primaryHandle, secondaryHandle)
            assertThat(actual).isEqualTo(false)
        }
}
