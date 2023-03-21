package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.login.FetchNodesBlockedAccount
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesUnknownStatus
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandles
import mega.privacy.android.domain.usecase.setting.ResetChatSettings
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class DefaultFetchNodesTest {

    private lateinit var underTest: FetchNodes

    private val establishCameraUploadsSyncHandles = mock<EstablishCameraUploadsSyncHandles>()
    private val loginRepository = mock<LoginRepository>()
    private val resetChatSettings = mock<ResetChatSettings>()

    @Before
    fun setUp() {
        underTest = DefaultFetchNodes(
            establishCameraUploadsSyncHandles = establishCameraUploadsSyncHandles,
            loginRepository = loginRepository,
            resetChatSettings = resetChatSettings,
            loginMutex = mock()
        )
    }

    @Test
    fun `test that fetch nodes invokes resetChatSettings if throws FetchNodesErrorAccess`() =
        runTest {
            whenever(loginRepository.fetchNodesFlow()).thenThrow(FetchNodesErrorAccess(mock()))

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(FetchNodesErrorAccess::class.java)
            }

            verify(loginRepository).fetchNodesFlow()
            verify(resetChatSettings).invoke()
        }

    @Test
    fun `test that fetch nodes invokes resetChatSettings if throws FetchNodesUnknownStatus`() =
        runTest {
            whenever(loginRepository.fetchNodesFlow()).thenThrow(FetchNodesUnknownStatus(mock()))

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(FetchNodesUnknownStatus::class.java)
            }

            verify(loginRepository).fetchNodesFlow()
            verify(resetChatSettings).invoke()
        }

    @Test
    fun `test that fetch nodes never invokes resetChatSettings if throws FetchNodesBlockedAccount`() =
        runTest {
            whenever(loginRepository.fetchNodesFlow()).thenThrow(FetchNodesBlockedAccount())

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(FetchNodesBlockedAccount::class.java)
            }

            verify(loginRepository).fetchNodesFlow()
            verifyNoInteractions(resetChatSettings)
        }

    @Test
    fun `test that fetch nodes success without error`() = runTest {
        val expectedUpdate = FetchNodesUpdate(mock(), mock())
        whenever(loginRepository.fetchNodesFlow()).thenReturn(flowOf(expectedUpdate))

        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(expectedUpdate)
            cancelAndIgnoreRemainingEvents()
        }

        verify(loginRepository).fetchNodesFlow()
    }

    @Test
    fun `test that successfully fetching all the nodes invokes establishCameraUploadsSyncHandles`() =
        runTest {
            val expectedUpdate = FetchNodesUpdate(Progress(1F), mock())
            whenever(loginRepository.fetchNodesFlow()).thenReturn(flowOf(expectedUpdate))

            underTest.invoke().test {
                assertThat(awaitItem()).isEqualTo(expectedUpdate)
                cancelAndIgnoreRemainingEvents()
            }

            verify(establishCameraUploadsSyncHandles).invoke()
        }
}