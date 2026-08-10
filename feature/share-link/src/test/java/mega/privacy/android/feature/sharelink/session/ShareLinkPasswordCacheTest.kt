package mega.privacy.android.feature.sharelink.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ShareLinkPasswordCacheTest {

    private val underTest = ShareLinkPasswordCache()

    @Test
    fun `test that get returns null when nothing is cached`() {
        assertThat(underTest.get(HANDLE)).isNull()
    }

    @Test
    fun `test that get returns the password after set`() {
        underTest.set(HANDLE, PASSWORD)

        assertThat(underTest.get(HANDLE)).isEqualTo(PASSWORD)
    }

    @Test
    fun `test that set with null removes the cached password`() {
        underTest.set(HANDLE, PASSWORD)

        underTest.set(HANDLE, null)

        assertThat(underTest.get(HANDLE)).isNull()
    }

    @Test
    fun `test that set overwrites the password for the same handle`() {
        underTest.set(HANDLE, PASSWORD)

        underTest.set(HANDLE, OTHER_PASSWORD)

        assertThat(underTest.get(HANDLE)).isEqualTo(OTHER_PASSWORD)
    }

    @Test
    fun `test that entries are kept independently per handle`() {
        underTest.set(HANDLE, PASSWORD)
        underTest.set(OTHER_HANDLE, OTHER_PASSWORD)

        assertThat(underTest.get(HANDLE)).isEqualTo(PASSWORD)
        assertThat(underTest.get(OTHER_HANDLE)).isEqualTo(OTHER_PASSWORD)
    }

    @Test
    fun `test that removing one handle leaves the other untouched`() {
        underTest.set(HANDLE, PASSWORD)
        underTest.set(OTHER_HANDLE, OTHER_PASSWORD)

        underTest.set(HANDLE, null)

        assertThat(underTest.get(HANDLE)).isNull()
        assertThat(underTest.get(OTHER_HANDLE)).isEqualTo(OTHER_PASSWORD)
    }

    @Test
    fun `test that monitor emits null when nothing is cached`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitor emits the password when set and null when removed`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isNull()

            underTest.set(HANDLE, PASSWORD)
            assertThat(awaitItem()).isEqualTo(PASSWORD)

            underTest.set(HANDLE, null)
            assertThat(awaitItem()).isNull()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitor does not re-emit when an unrelated handle changes`() = runTest {
        underTest.monitor(HANDLE).test {
            assertThat(awaitItem()).isNull()

            underTest.set(OTHER_HANDLE, OTHER_PASSWORD)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val HANDLE = 123L
        const val OTHER_HANDLE = 456L
        val PASSWORD = LinkPassword(password = "Str0ngP@ss", linkWithPassword = "https://mega.nz/#P!enc")
        val OTHER_PASSWORD = LinkPassword(password = "An0therP@ss", linkWithPassword = null)
    }
}
