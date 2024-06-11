package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserLastGreen
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [UserLastGreenMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserLastGreenMapperTest {
    private lateinit var underTest: UserLastGreenMapper

    @BeforeAll
    fun setUp() {
        underTest = UserLastGreenMapper()
    }

    @Test
    fun `test that the parameters are mapped into a user last green object`() = runTest {
        val handle = 123456L
        val lastGreen = 50

        val expected = UserLastGreen(handle = handle, lastGreen = lastGreen)
        val actual = underTest(handle = handle, lastGreen = lastGreen)

        assertThat(actual).isEqualTo(expected)
    }
}