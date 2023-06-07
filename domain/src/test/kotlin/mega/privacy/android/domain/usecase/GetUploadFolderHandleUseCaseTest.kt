package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUploadFolderHandleUseCaseTest {
    private lateinit var underTest: GetUploadFolderHandleUseCase
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetUploadFolderHandleUseCase(
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(getPrimarySyncHandleUseCase, getSecondarySyncHandleUseCase)
    }


    @Test
    fun `test that invoke with true returns primary upload folder handle`() = runTest {
        val primaryHandle = 123456789L
        whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryHandle)
        assertThat(underTest(CameraUploadFolderType.Primary)).isEqualTo(primaryHandle)
    }

    @Test
    fun `test that invoke with false returns secondary upload folder handle`() = runTest {
        val secondaryHandle = 123456789L
        whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryHandle)
        assertThat(underTest(CameraUploadFolderType.Secondary)).isEqualTo(secondaryHandle)
    }
}
