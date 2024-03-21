package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsAMegaContactByEmailUseCaseTest {

    private lateinit var underTest: IsAMegaContactByEmailUseCase

    private val userEmail = "user@test.com"

    @BeforeEach
    fun setup() {
        underTest = IsAMegaContactByEmailUseCase()
    }

    @Test
    fun `test that true is returned when the user's email already exists in MEGA contacts`() {
        val user = User(
            handle = 1L,
            email = userEmail,
            visibility = UserVisibility.Visible,
            timestamp = System.currentTimeMillis(),
            userChanges = listOf()
        )

        val actual = underTest(user, this.userEmail)

        assertThat(actual).isTrue()
    }

    private val localEmail = "local@test.com"

    @ParameterizedTest
    @MethodSource("provideInvalidData")
    fun `test that false is returned when`(
        localEmail: String,
        userEmail: String,
        userVisibility: UserVisibility,
    ) {
        val user = User(
            handle = 1L,
            email = userEmail,
            visibility = userVisibility,
            timestamp = System.currentTimeMillis(),
            userChanges = listOf()
        )

        val actual = underTest(user, localEmail)

        assertThat(actual).isFalse()
    }

    private fun provideInvalidData() = Stream.of(
        Arguments.of(localEmail, userEmail, UserVisibility.Visible),
        Arguments.of(userEmail, userEmail, UserVisibility.Unknown),
        Arguments.of(localEmail, userEmail, UserVisibility.Unknown)
    )
}
