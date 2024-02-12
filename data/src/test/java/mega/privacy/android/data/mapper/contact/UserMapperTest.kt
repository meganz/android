package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserMapperTest {
    private lateinit var underTest: UserMapper
    private val userChangeMapper: UserChangeMapper = mock<UserChangeMapper>() {
        on { invoke(MegaUser.CHANGE_TYPE_AUTHRING.toLong()) }.thenReturn(listOf(UserChanges.AuthenticationInformation))
    }

    @BeforeAll
    fun setup() {
        underTest = UserMapper(userChangeMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            userChangeMapper,
        )
    }

    @Test
    fun `test that invoke returns null when megaUser is null`() {
        assertThat(underTest(null)).isNull()
    }

    @Test
    fun `test that invoke returns User`() {
        val megaUser = mock<MegaUser> {
            on { handle }.thenReturn(123L)
            on { email }.thenReturn("email")
            on { visibility }.thenReturn(MegaUser.VISIBILITY_VISIBLE)
            on { timestamp }.thenReturn(123L)
            on { changes }.thenReturn(MegaUser.CHANGE_TYPE_AUTHRING.toLong())
        }
        whenever(userChangeMapper(MegaUser.CHANGE_TYPE_AUTHRING.toLong())).thenReturn(
            listOf(
                UserChanges.AuthenticationInformation
            )
        )
        val expectedUser = User(
            handle = megaUser.handle,
            email = megaUser.email,
            visibility = UserVisibility.Visible,
            timestamp = megaUser.timestamp,
            userChanges = userChangeMapper(megaUser.changes)
        )

        assertThat(underTest(megaUser)).isEqualTo(expectedUser)
    }
}
