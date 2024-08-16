package mega.privacy.android.app.presentation.login.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginStateTest {
    @Test
    fun `test that currentStatusText returns correct string resource by default`() {
        val loginState = LoginState()
        assertThat(loginState.currentStatusText).isEqualTo(R.string.login_connecting_to_server)
    }

    @Test
    fun `test that currentStatusText returns correct string resource when first login`() {
        val loginState =
            LoginState(isFirstTime = true, fetchNodesUpdate = FetchNodesUpdate(Progress(0.2f)))
        assertThat(loginState.currentStatusText).isEqualTo(R.string.login_preparing_filelist)
    }

    @Test
    fun `test that currentProgress returns correct progress by default`() {
        val loginState = LoginState()
        assertThat(loginState.currentProgress).isEqualTo(0.5f)
    }

    @Test
    fun `test that currentProgress returns correct progress when checking signup link`() {
        val loginState = LoginState(isCheckingSignupLink = true)
        assertThat(loginState.currentProgress).isEqualTo(0.9f)
    }

    @Test
    fun `test that currentProgress returns correct progress when fast login in progress`() {
        val loginState = LoginState(isFastLoginInProgress = true)
        assertThat(loginState.currentProgress).isEqualTo(0.5f)
    }

    @Test
    fun `test that currentProgress returns correct progress when fetch nodes update is not null`() {
        val fetchNodesUpdate = mock(FetchNodesUpdate::class.java)
        val loginState = LoginState(fetchNodesUpdate = fetchNodesUpdate, isFirstTime = true)
        assertThat(loginState.currentProgress).isEqualTo(0.4f)
    }

    @Test
    fun `test that currentProgress returns correct progress when fetch nodes update progress is not null`() {
        val fetchNodesUpdate = mock(FetchNodesUpdate::class.java)
        whenever(fetchNodesUpdate.progress).thenReturn(mock(Progress::class.java))
        whenever(fetchNodesUpdate.progress?.floatValue).thenReturn(0.5f)
        val loginState = LoginState(fetchNodesUpdate = fetchNodesUpdate, isFirstTime = true)
        assertThat(loginState.currentProgress).isEqualTo(0.70000005f)
    }

    @Test
    fun `test that currentProgress returns correct progress when fetch nodes update progress is not null and not first login`() {
        val fetchNodesUpdate = mock(FetchNodesUpdate::class.java)
        whenever(fetchNodesUpdate.progress).thenReturn(mock(Progress::class.java))
        whenever(fetchNodesUpdate.progress?.floatValue).thenReturn(0.5f)
        val loginState = LoginState(fetchNodesUpdate = fetchNodesUpdate, isFirstTime = false)
        assertThat(loginState.currentProgress).isEqualTo(0.975f)
    }
}