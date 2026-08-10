package mega.privacy.android.data.test.state

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeAccountStateTest {

    @Test
    fun `test that the account describes a logged in user when created`() {
        val underTest = FakeAccountState()

        assertThat(underTest.isLoggedIn).isTrue()
        assertThat(underTest.email).isEqualTo("test@mega.nz")
        assertThat(underTest.myUserHandle).isEqualTo(111L)
        assertThat(underTest.session).isEqualTo("fake-session")
        assertThat(underTest.isBusinessAccount).isFalse()
        assertThat(underTest.isMasterBusinessAccount).isFalse()
        assertThat(underTest.isBusinessAccountActive).isTrue()
        assertThat(underTest.businessStatus).isEqualTo(0)
        assertThat(underTest.isAchievementsEnabled).isTrue()
        assertThat(underTest.isEphemeralPlusPlus).isFalse()
        assertThat(underTest.myCredentials).isEqualTo("fake-credentials")
    }

    @Test
    fun `test that reset restores the defaults when fields were mutated`() {
        val underTest = FakeAccountState().apply {
            isLoggedIn = false
            email = "other@mega.nz"
            myUserHandle = 999L
            session = "other-session"
            isBusinessAccount = true
            isMasterBusinessAccount = true
            isBusinessAccountActive = false
            businessStatus = 2
            isAchievementsEnabled = false
            isEphemeralPlusPlus = true
            myCredentials = null
        }

        underTest.reset()

        assertThat(underTest.isLoggedIn).isTrue()
        assertThat(underTest.email).isEqualTo("test@mega.nz")
        assertThat(underTest.myUserHandle).isEqualTo(111L)
        assertThat(underTest.session).isEqualTo("fake-session")
        assertThat(underTest.isBusinessAccount).isFalse()
        assertThat(underTest.isMasterBusinessAccount).isFalse()
        assertThat(underTest.isBusinessAccountActive).isTrue()
        assertThat(underTest.businessStatus).isEqualTo(0)
        assertThat(underTest.isAchievementsEnabled).isTrue()
        assertThat(underTest.isEphemeralPlusPlus).isFalse()
        assertThat(underTest.myCredentials).isEqualTo("fake-credentials")
    }
}
