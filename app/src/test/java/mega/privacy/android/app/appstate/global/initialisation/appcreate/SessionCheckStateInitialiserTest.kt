package mega.privacy.android.app.appstate.global.initialisation.appcreate

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.account.MonitorUserCredentialsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.MonitorLogoutUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionCheckStateInitialiserTest {
    private lateinit var underTest: SessionCheckStateInitialiser

    private val rootNodeExistsUseCase = mock<RootNodeExistsUseCase>()
    private val monitorFetchNodesFinishUseCase = mock<MonitorFetchNodesFinishUseCase>()
    private val monitorLogoutUseCase = mock<MonitorLogoutUseCase>()
    private val monitorUserCredentialsUseCase = mock<MonitorUserCredentialsUseCase>()

    private val credentials = UserCredentials(
        email = "email@example.com",
        session = "session",
        firstName = "First",
        lastName = "Last",
        myHandle = "handle",
    )

    @BeforeEach
    fun setUp() {
        reset(
            rootNodeExistsUseCase,
            monitorFetchNodesFinishUseCase,
            monitorLogoutUseCase,
            monitorUserCredentialsUseCase,
        )
        rootNodeExistsUseCase.stub { onBlocking { invoke() }.thenReturn(false) }
        monitorFetchNodesFinishUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorLogoutUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        monitorUserCredentialsUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }

        underTest = SessionCheckStateInitialiser(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            monitorLogoutUseCase = monitorLogoutUseCase,
            monitorUserCredentialsUseCase = monitorUserCredentialsUseCase,
        )
    }

    @Test
    fun `test that rootNodeExists and userCredentials are null before invoke`() {
        assertThat(underTest.rootNodeExists.value).isNull()
        assertThat(underTest.userCredentials.value).isNull()
    }

    @Test
    fun `test that invoke sets rootNodeExists from the use case`() = runTest {
        rootNodeExistsUseCase.stub { onBlocking { invoke() }.thenReturn(true) }

        underTest()

        assertThat(underTest.rootNodeExists.value).isTrue()
    }

    @Test
    fun `test that rootNodeExists is recomputed when fetch nodes finishes`() = runTest {
        rootNodeExistsUseCase.stub { onBlocking { invoke() }.thenReturn(false, true) }
        monitorFetchNodesFinishUseCase.stub { on { invoke() }.thenReturn(flowOf(true)) }

        underTest()

        assertThat(underTest.rootNodeExists.value).isTrue()
    }

    @Test
    fun `test that rootNodeExists is recomputed when a logout occurs`() = runTest {
        rootNodeExistsUseCase.stub { onBlocking { invoke() }.thenReturn(true, false) }
        monitorLogoutUseCase.stub { on { invoke() }.thenReturn(flowOf(true)) }

        underTest()

        assertThat(underTest.rootNodeExists.value).isFalse()
    }

    @Test
    fun `test that rootNodeExists is null when the check fails`() = runTest {
        rootNodeExistsUseCase.stub { onBlocking { invoke() }.thenThrow(RuntimeException()) }

        underTest()

        assertThat(underTest.rootNodeExists.value).isNull()
    }

    @Test
    fun `test that userCredentials mirrors the credentials flow`() = runTest {
        monitorUserCredentialsUseCase.stub { on { invoke() }.thenReturn(flowOf(credentials)) }

        underTest()

        assertThat(underTest.userCredentials.value).isEqualTo(credentials)
    }

    @Test
    fun `test that userCredentials is null when the credentials flow emits null`() = runTest {
        monitorUserCredentialsUseCase.stub { on { invoke() }.thenReturn(flowOf(credentials, null)) }

        underTest()

        assertThat(underTest.userCredentials.value).isNull()
    }
}
