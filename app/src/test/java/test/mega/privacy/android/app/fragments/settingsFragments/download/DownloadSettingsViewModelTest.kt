package test.mega.privacy.android.app.fragments.settingsFragments.download

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.fragments.settingsFragments.download.DownloadSettingsViewModel
import mega.privacy.android.domain.usecase.GetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPath
import mega.privacy.android.domain.usecase.GetStorageDownloadLocation
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlways
import mega.privacy.android.domain.usecase.SetStorageDownloadLocation
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadSettingsViewModelTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    lateinit var underTest: DownloadSettingsViewModel
    private val getStorageDownloadLocation = mock<GetStorageDownloadLocation>()
    private val setStorageDownloadLocation = mock<SetStorageDownloadLocation>()
    private val getStorageAskAlways = mock<GetStorageDownloadAskAlways>()
    private val setStorageAskAlways = mock<SetStorageDownloadAskAlways>()
    private val getStorageDownloadDefaultPath = mock<GetStorageDownloadDefaultPath>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = constructViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun constructViewModel() = DownloadSettingsViewModel(
        setStorageAskAlways,
        setStorageDownloadLocation,
        getStorageAskAlways,
        getStorageDownloadLocation,
        getStorageDownloadDefaultPath
    )

    @Test
    fun `test that the initial state is returned when the view model is first loaded`() = runTest {
        val expectedAskAlways = Random.nextBoolean()
        val expectedPath = "/expected/path"
        whenever(getStorageAskAlways()).thenReturn(expectedAskAlways)
        whenever(getStorageDownloadLocation()).thenReturn(expectedPath)

        underTest = constructViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().isAskAlwaysChecked).isEqualTo(expectedAskAlways)
        }
        verify(setStorageDownloadLocation)(expectedPath)
    }

    @Test
    fun `test that the download location path state is updated when download location is not null`() =
        runTest {
            val expectedPath = "/expected/path"

            underTest.setDownloadLocation(expectedPath)

            underTest.uiState.test {
                assertThat(awaitItem().downloadLocationPath).isEqualTo(expectedPath)
            }
            verify(setStorageDownloadLocation)(expectedPath)
        }

    @Test
    fun `test that the ask me checked state is updated when setStorageDownloadAskAlwaysUseCase is invoked`() =
        runTest {
            fun verify(isChecked: Boolean) = launch {
                underTest.onStorageAskAlwaysChanged(isChecked = isChecked)

                verify(setStorageAskAlways)(isChecked)

                underTest.uiState.test {
                    assertThat(awaitItem().isAskAlwaysChecked).isEqualTo(isChecked)
                }
            }

            verify(isChecked = true)
            verify(isChecked = false)
        }
}