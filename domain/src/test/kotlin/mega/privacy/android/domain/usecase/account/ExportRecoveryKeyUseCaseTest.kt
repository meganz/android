package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.file.SaveTextOnContentUriUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ExportRecoveryKeyUseCaseTest {
    private lateinit var underTest: ExportRecoveryKeyUseCase
    private val getExportMasterKeyUseCase = mock<GetExportMasterKeyUseCase>()
    private val setMasterKeyExportedUseCase = mock<SetMasterKeyExportedUseCase>()
    private val saveTextOnContentUriUseCase = mock<SaveTextOnContentUriUseCase>()
    private val uri = "testUri"

    @Before
    fun setUp() {
        underTest = ExportRecoveryKeyUseCase(
            getExportMasterKeyUseCase = getExportMasterKeyUseCase,
            setMasterKeyExportedUseCase = setMasterKeyExportedUseCase,
            saveTextOnContentUriUseCase = saveTextOnContentUriUseCase,
        )
    }

    @Test
    fun `test that false is returned when getExportMasterKeyUseCase returns null key`() {
        runTest {
            whenever(getExportMasterKeyUseCase()).thenReturn(null)
            val result = underTest(uri)
            assertThat(result).isEqualTo(false)
        }
    }

    @Test
    fun `test that setMasterKeyExportedUseCase and saveTextOnContentUriUseCase is invoked when getExportMasterKeyUseCase returns valid key`() =
        runTest {
            val key = "testKey"
            whenever(getExportMasterKeyUseCase()).thenReturn(key)
            underTest(uri)
            verify(setMasterKeyExportedUseCase).invoke()
            verify(saveTextOnContentUriUseCase).invoke(uri, key)
        }
}