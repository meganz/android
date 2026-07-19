package mega.privacy.android.feature.sharelink.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ShareLinkSeparateKeyCacheTest {

    private val underTest = ShareLinkSeparateKeyCache()

    @Test
    fun `test that get returns false when nothing is cached`() {
        assertThat(underTest.get(HANDLE)).isFalse()
    }

    @Test
    fun `test that get returns true after set true`() {
        underTest.set(HANDLE, true)

        assertThat(underTest.get(HANDLE)).isTrue()
    }

    @Test
    fun `test that set false clears the cached preference`() {
        underTest.set(HANDLE, true)

        underTest.set(HANDLE, false)

        assertThat(underTest.get(HANDLE)).isFalse()
    }

    @Test
    fun `test that preferences are kept independently per handle`() {
        underTest.set(HANDLE, true)

        assertThat(underTest.get(HANDLE)).isTrue()
        assertThat(underTest.get(OTHER_HANDLE)).isFalse()
    }

    @Test
    fun `test that monitor emits false when nothing is cached`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitor emits true when set and false when cleared`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isFalse()

            underTest.set(HANDLE, true)
            assertThat(awaitItem()).isTrue()

            underTest.set(HANDLE, false)
            assertThat(awaitItem()).isFalse()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitor does not re-emit when an unrelated handle changes`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isFalse()

            underTest.set(OTHER_HANDLE, true)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val HANDLE = 123L
        const val OTHER_HANDLE = 456L
    }
}
