package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsEmailValidUseCaseTest {

    private val underTest = IsEmailValidUseCase()

    @ParameterizedTest
    @ValueSource(strings = ["test@test.com", "test+.%@test.com", "test&@a-test.c"])
    fun `test that true is returned given a valid email`(email: String) {
        assertThat(underTest(email)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["test@.com", "test.com", "test", "test@.", "", " "])
    fun `test that false is returned given an invalid email`(email: String) {
        assertThat(underTest(email)).isFalse()
    }
}
