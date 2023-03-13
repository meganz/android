package test.mega.privacy.android.app.presentation.folderlink

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.folderlink.FolderLinkViewModel
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.folderlink.LoginToFolder
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class FolderLinkViewModelTest {

    private lateinit var underTest: FolderLinkViewModel
    private val monitorViewType = mock<MonitorViewType>()
    private val monitorConnectivity = mock<MonitorConnectivity>()
    private val loginToFolder = mock<LoginToFolder>()
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase = mock()
    private val copyNodeUseCase: CopyNodeUseCase = mock()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        underTest = FolderLinkViewModel(
            monitorConnectivity,
            monitorViewType,
            loginToFolder,
            checkNameCollisionUseCase,
            copyNodeUseCase
        )
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.isInitialState).isEqualTo(true)
            assertThat(initial.isLoginComplete).isEqualTo(false)
            assertThat(initial.isNodesFetched).isEqualTo(false)
            assertThat(initial.askForDecryptionKeyDialog).isEqualTo(false)
            assertThat(initial.collisions).isNull()
            assertThat(initial.copyResultText).isNull()
            assertThat(initial.copyThrowable).isNull()
            assertThat(initial.errorDialogTitle).isEqualTo(-1)
            assertThat(initial.errorDialogContent).isEqualTo(-1)
            assertThat(initial.snackBarMessage).isEqualTo(-1)
        }
    }

    @Test
    fun `test that isNodesFetched updated correctly`() = runTest {
        underTest.state.map { it.isNodesFetched }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            underTest.updateIsNodesFetched(true)
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that on login into folder and on result OK values are updated correctly`() = runTest {
        val folderLink = "abcd"
        whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.SUCCESS)
        underTest.state.test {
            underTest.folderLogin(folderLink)
            val newValue = expectMostRecentItem()
            assertThat(newValue.isLoginComplete).isTrue()
            assertThat(newValue.isInitialState).isFalse()
        }
    }

    @Test
    fun `test that on login into folder and on result API_INCOMPLETE values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.API_INCOMPLETE)
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isTrue()
            }
        }

    @Test
    fun `test that on login into folder and on result INCORRECT_KEY values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            val decryptionIntroduced = true
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.INCORRECT_KEY)
            underTest.state.test {
                underTest.folderLogin(folderLink, decryptionIntroduced)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isEqualTo(decryptionIntroduced)
            }
        }

    @Test
    fun `test that on login into folder and on result ERROR values are updated correctly`() =
        runTest {
            val folderLink = "abcd"
            whenever(loginToFolder(folderLink)).thenReturn(FolderLoginStatus.ERROR)
            underTest.state.test {
                underTest.folderLogin(folderLink)
                val newValue = expectMostRecentItem()
                assertThat(newValue.isLoginComplete).isFalse()
                assertThat(newValue.isInitialState).isFalse()
                assertThat(newValue.askForDecryptionKeyDialog).isFalse()
            }
        }

    @Test
    fun `test that launchCollisionActivity values are reset `() = runTest {
        underTest.state.test {
            underTest.resetLaunchCollisionActivity()
            val newValue = expectMostRecentItem()
            assertThat(newValue.collisions).isNull()
        }
    }

    @Test
    fun `test that showCopyResult values are reset `() = runTest {
        underTest.state.test {
            underTest.resetShowCopyResult()
            val newValue = expectMostRecentItem()
            assertThat(newValue.copyResultText).isNull()
            assertThat(newValue.copyThrowable).isNull()
        }
    }
}