package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserVisibility
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.spy
import org.mockito.kotlin.mock

/**
 * Test class for [UserMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserMapperTest {
    private lateinit var underTest: UserMapper

    private val userChangeMapper: UserChangeMapper = spy(UserChangeMapper())

    @BeforeAll
    fun setup() {
        underTest = UserMapper(userChangeMapper)
    }

    @TestFactory
    fun `test that a mega user is mapped to a user with the correct user visibility`() = listOf(
        MegaUser.VISIBILITY_UNKNOWN to UserVisibility.Unknown,
        MegaUser.VISIBILITY_HIDDEN to UserVisibility.Hidden,
        MegaUser.VISIBILITY_VISIBLE to UserVisibility.Visible,
        MegaUser.VISIBILITY_INACTIVE to UserVisibility.Inactive,
        MegaUser.VISIBILITY_BLOCKED to UserVisibility.Blocked,
    ).map { (inputVisibility, expectedVisibility) ->
        dynamicTest("test that $inputVisibility is mapped to $expectedVisibility") {
            val megaUser = mock<MegaUser> {
                on { handle }.thenReturn(123L)
                on { email }.thenReturn("email")
                on { visibility }.thenReturn(inputVisibility)
                on { timestamp }.thenReturn(123L)
                on { changes }.thenReturn(MegaUser.CHANGE_TYPE_AUTHRING.toLong())
            }

            val expected = User(
                handle = megaUser.handle,
                email = megaUser.email,
                visibility = expectedVisibility,
                timestamp = megaUser.timestamp,
                userChanges = listOf(UserChanges.AuthenticationInformation)
            )
            val actual = underTest(megaUser)

            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test that a mega user with an unspecified user visibility is mapped to a user with an unknown user visibility`() =
        runTest {
            val megaUser = mock<MegaUser> {
                on { handle }.thenReturn(123L)
                on { email }.thenReturn("email")
                // 100 is unspecified in the Mapper
                on { visibility }.thenReturn(100)
                on { timestamp }.thenReturn(123L)
                on { changes }.thenReturn(MegaUser.CHANGE_TYPE_AUTHRING.toLong())
            }

            val expected = User(
                handle = megaUser.handle,
                email = megaUser.email,
                visibility = UserVisibility.Unknown,
                timestamp = megaUser.timestamp,
                userChanges = listOf(UserChanges.AuthenticationInformation)
            )
            val actual = underTest(megaUser)

            assertThat(actual).isEqualTo(expected)
        }
}
