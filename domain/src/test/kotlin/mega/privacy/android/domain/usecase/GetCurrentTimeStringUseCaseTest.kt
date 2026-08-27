package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCurrentTimeStringUseCaseTest {
    private lateinit var underTest: GetCurrentTimeStringUseCase

    private val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

    @BeforeAll
    fun setUp() {
        underTest = GetCurrentTimeStringUseCase()
    }

    @Test
    fun `test that invoke formats the current time as month-day hours-minutes-seconds`() {
        assertThat(underTest()).matches("""\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")
    }

    @Test
    fun `test that invoke formats the current time in UTC`() {
        val before = formatter.format(ZonedDateTime.now(ZoneOffset.UTC))

        val actual = underTest()

        val after = formatter.format(ZonedDateTime.now(ZoneOffset.UTC))
        assertThat(actual).isIn(setOf(before, after))
    }
}
