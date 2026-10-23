package mega.privacy.android.app.appstate.content.navigation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NavigationResultManagerTest {
    private lateinit var underTest: NavigationResultManager

    private val key = "key"

    @BeforeEach
    fun setUp() {
        underTest = NavigationResultManager()
    }

    @Test
    fun `test that takeResult returns the stored result`() {
        underTest.returnResult(key, "value")

        assertThat(underTest.takeResult<String>(key)).isEqualTo("value")
    }

    @Test
    fun `test that takeResult returns null for the second caller`() {
        underTest.returnResult(key, "value")

        underTest.takeResult<String>(key)

        // The guarantee the node options sheet relies on: several screens are composed at once and
        // each of them reacts to the same result, so only the first may receive it.
        assertThat(underTest.takeResult<String>(key)).isNull()
    }

    @Test
    fun `test that takeResult returns null when no result is stored`() {
        assertThat(underTest.takeResult<String>(key)).isNull()
    }

    @Test
    fun `test that takeResult clears the monitored value`() = runTest {
        underTest.returnResult(key, "value")

        underTest.takeResult<String>(key)

        assertThat(underTest.monitorResult<String>(key).first()).isNull()
    }

    @Test
    fun `test that takeResult returns the result stored after an earlier one was claimed`() {
        underTest.returnResult(key, "first")
        underTest.takeResult<String>(key)

        underTest.returnResult(key, "second")

        assertThat(underTest.takeResult<String>(key)).isEqualTo("second")
    }
}
