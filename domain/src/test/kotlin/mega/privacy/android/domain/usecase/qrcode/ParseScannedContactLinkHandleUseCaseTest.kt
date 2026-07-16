package mega.privacy.android.domain.usecase.qrcode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParseScannedContactLinkHandleUseCaseTest {

    private lateinit var underTest: ParseScannedContactLinkHandleUseCase

    @BeforeAll
    fun setUp() {
        underTest = ParseScannedContactLinkHandleUseCase()
    }

    @Test
    fun `test that invoke returns handle when scanned code is a mega nz contact link`() = runTest {
        assertThat(underTest("https://mega.nz/C!wf8jTYRB")).isEqualTo("wf8jTYRB")
    }

    @Test
    fun `test that invoke returns handle when scanned code is a mega app contact link`() = runTest {
        assertThat(underTest("https://mega.app/C!wf8jTYRB")).isEqualTo("wf8jTYRB")
    }

    @Test
    fun `test that invoke returns null when domain is not a mega domain`() = runTest {
        assertThat(underTest("https://mega.io/C!wf8jTYRB")).isNull()
    }

    @Test
    fun `test that invoke returns null when scheme is not https`() = runTest {
        assertThat(underTest("http://mega.nz/C!wf8jTYRB")).isNull()
    }

    @Test
    fun `test that invoke returns null when contact link prefix is missing`() = runTest {
        assertThat(underTest("https://mega.nz/wf8jTYRB")).isNull()
    }

    @Test
    fun `test that invoke returns null when scanned code is garbage`() = runTest {
        assertThat(underTest("not a link at all")).isNull()
    }

    @Test
    fun `test that invoke returns null when scanned code is empty`() = runTest {
        assertThat(underTest("")).isNull()
    }

    @Test
    fun `test that invoke returns null when prefix appears without a base url`() = runTest {
        assertThat(underTest("C!wf8jTYRB")).isNull()
    }

    @Test
    fun `test that invoke returns text between first and second prefix when handle contains the prefix`() =
        runTest {
            assertThat(underTest("https://mega.nz/C!abC!def")).isEqualTo("ab")
        }
}
