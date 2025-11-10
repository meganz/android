package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nz.mega.sdk.MegaApiAndroid
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [MegaApiFacade]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaApiFacadeTest {

    private lateinit var underTest: MegaApiFacade
    private val megaApi: MegaApiAndroid = mock()
    private val sharingScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {
        underTest = MegaApiFacade(
            megaApi = megaApi,
            sharingScope = sharingScope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(megaApi)
    }

    @Test
    fun `test that isNodeS4Container returns false when S4 is not enabled`() = runTest {
        whenever(megaApi.isS4Enabled).thenReturn(false)
        val nodeHandle = 12345L

        val result = underTest.isNodeS4Container(nodeHandle)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isNodeS4Container returns false when node handle is invalid`() = runTest {
        whenever(megaApi.isS4Enabled).thenReturn(true)

        val result = underTest.isNodeS4Container(MegaApiAndroid.INVALID_HANDLE)

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isNodeS4Container returns false when S4 container handle is invalid`() =
        runTest {
            whenever(megaApi.isS4Enabled).thenReturn(true)
            whenever(megaApi.s4Container).thenReturn(MegaApiAndroid.INVALID_HANDLE)
            val nodeHandle = 12345L

            val result = underTest.isNodeS4Container(nodeHandle)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that isNodeS4Container returns false when node handle does not match S4 container handle`() =
        runTest {
            whenever(megaApi.isS4Enabled).thenReturn(true)
            whenever(megaApi.s4Container).thenReturn(12345L)
            val nodeHandle = 67890L

            val result = underTest.isNodeS4Container(nodeHandle)

            assertThat(result).isFalse()
        }

    @Test
    fun `test that isNodeS4Container returns true when node handle matches S4 container handle`() =
        runTest {
            val s4ContainerHandle = 12345L
            whenever(megaApi.isS4Enabled).thenReturn(true)
            whenever(megaApi.s4Container).thenReturn(s4ContainerHandle)

            val result = underTest.isNodeS4Container(s4ContainerHandle)

            assertThat(result).isTrue()
        }
}

