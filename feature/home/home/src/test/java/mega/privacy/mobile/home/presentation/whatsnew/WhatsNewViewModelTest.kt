package mega.privacy.mobile.home.presentation.whatsnew

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AppVersion
import mega.privacy.android.domain.usecase.global.GetAppVersionUseCase
import mega.privacy.android.domain.usecase.home.MarkNewFeatureDisplayedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class WhatsNewViewModelTest {

    private lateinit var underTest: WhatsNewViewModel

    private val getAppVersionUseCase = mock<GetAppVersionUseCase>()
    private val whatsNewDetail = mock<WhatsNewDetail>()
    private val markNewFeatureDisplayedUseCase: MarkNewFeatureDisplayedUseCase = mock()

    @BeforeEach
    fun setUp() {
        reset(
            getAppVersionUseCase,
            whatsNewDetail,
            markNewFeatureDisplayedUseCase
        )
    }

    private fun initViewModel(
        whatsNewDetails: Map<String, WhatsNewDetail> = emptyMap(),
    ) {
        underTest = WhatsNewViewModel(
            whatsNewDetails = whatsNewDetails,
            getAppVersionUseCase = getAppVersionUseCase,
            markNewFeatureDisplayedUseCase = markNewFeatureDisplayedUseCase
        )
    }

    @Test
    fun `test that initial state is Loading when no version matches`() = runTest {
        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Loading)
        }
    }

    @Test
    fun `test that uiState emits Ready when version matches`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(AppVersion(16, 1, null))

        initViewModel(whatsNewDetails = mapOf("16.1" to whatsNewDetail))

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Ready(whatsNewDetail))
        }
    }

    @Test
    fun `test that uiState stays Loading when version does not match`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(AppVersion(15, 0, null))

        initViewModel(whatsNewDetails = mapOf("16.1" to whatsNewDetail))

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Loading)
        }
    }

    @Test
    fun `test that uiState stays Loading when app version is null`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(null)

        initViewModel(whatsNewDetails = mapOf("16.1" to whatsNewDetail))

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Loading)
        }
    }

    @Test
    fun `test that uiState stays Loading when details map is empty`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(AppVersion(16, 1, null))

        initViewModel(whatsNewDetails = emptyMap())

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Loading)
        }
    }

    @Test
    fun `test that version string uses major and minor only`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(AppVersion(17, 3, 2))

        initViewModel(whatsNewDetails = mapOf("17.3" to whatsNewDetail))

        underTest.uiState.test {
            val state = expectMostRecentItem()
            assertThat(state).isEqualTo(WhatsNewUiState.Ready(whatsNewDetail))
        }
    }

    @Test
    fun `test that markNewFeatureDisplayedUseCase is called when version matches`() = runTest {
        whenever(getAppVersionUseCase()).thenReturn(AppVersion(16, 1, null))

        initViewModel(whatsNewDetails = mapOf("16.1" to whatsNewDetail))

        underTest.uiState.test {
            expectMostRecentItem()
        }

        verify(markNewFeatureDisplayedUseCase).invoke()
    }
}
