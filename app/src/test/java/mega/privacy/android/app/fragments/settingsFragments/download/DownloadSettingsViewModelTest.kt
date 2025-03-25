package mega.privacy.android.app.fragments.settingsFragments.download

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import mega.privacy.android.domain.usecase.GetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.IsStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.file.GetExternalPathByContentUriUseCase
import mega.privacy.android.domain.usecase.file.IsExternalStorageContentUriUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadSettingsViewModelTest {
    private lateinit var underTest: DownloadSettingsViewModel

    private val setStorageDownloadAskAlwaysUseCase = mock<SetStorageDownloadAskAlwaysUseCase>()
    private val setStorageDownloadLocationUseCase = mock<SetStorageDownloadLocationUseCase>()
    private val isStorageDownloadAskAlwaysUseCase = mock<IsStorageDownloadAskAlwaysUseCase>()
    private val getStorageDownloadLocationUseCase = mock<GetStorageDownloadLocationUseCase>()
    private val getStorageDownloadDefaultPathUseCase = mock<GetStorageDownloadDefaultPathUseCase>()
    private val isExternalStorageContentUriUseCase = mock<IsExternalStorageContentUriUseCase>()
    private val getExternalPathByContentUriUseCase = mock<GetExternalPathByContentUriUseCase>()

    @BeforeAll
    fun setUp() = runTest {
        whenever(isStorageDownloadAskAlwaysUseCase()) doReturn false
        underTest = DownloadSettingsViewModel(
            setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase,
            isStorageDownloadAskAlwaysUseCase,
            getStorageDownloadLocationUseCase,
            getStorageDownloadDefaultPathUseCase,
            isExternalStorageContentUriUseCase,
            getExternalPathByContentUriUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            setStorageDownloadAskAlwaysUseCase,
            setStorageDownloadLocationUseCase,
            isStorageDownloadAskAlwaysUseCase,
            getStorageDownloadLocationUseCase,
            getStorageDownloadDefaultPathUseCase,
            isExternalStorageContentUriUseCase,
            getExternalPathByContentUriUseCase,
        )
    }

    @Test
    fun `test that download location is saved when not null value is provided`() = runTest {
        val expected = "download location"
        whenever(isExternalStorageContentUriUseCase(any())) doReturn false

        underTest.setDownloadLocation(expected)

        verify(setStorageDownloadLocationUseCase)(expected)
    }

    @Test
    fun `test that default download location is saved when null is provided`() = runTest {
        val expected = "download location"
        whenever(isExternalStorageContentUriUseCase(any())) doReturn false
        whenever(getStorageDownloadDefaultPathUseCase()) doReturn expected

        underTest.setDownloadLocation(null)

        verify(setStorageDownloadLocationUseCase)(expected)
    }

    @Test
    fun `test that ui state is updated when download location is changed`() = runTest {
        val expected = "download location"
        whenever(isExternalStorageContentUriUseCase(any())) doReturn false

        underTest.setDownloadLocation(expected)

        assertThat(underTest.uiState.value.downloadLocationPath).isEqualTo(expected)
    }

    @Test
    fun `test that ui state is updated with path when download location is updated with a content uri`() =
        runTest {
            val contentUri = "content://external.foo"
            val expected = "download location path"
            whenever(isExternalStorageContentUriUseCase(contentUri)) doReturn true
            whenever(getExternalPathByContentUriUseCase(contentUri)) doReturn expected

            underTest.setDownloadLocation(contentUri)

            assertThat(underTest.uiState.value.downloadLocationPath).isEqualTo(expected)
        }
}