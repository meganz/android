package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test

/**
 * test case for [QRCodeFacade]
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QRCodeFacadeTest {

    private lateinit var underTest: QRCodeFacade
    private val testCoroutineDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest = QRCodeFacade(testCoroutineDispatcher)
    }

    @Test
    fun `test that no QRCode is generated for empty text`() = runTest {
        assertThat(underTest.createQRCode("", 1000, 1000)).isNull()
    }

    @Test
    fun `test that QRCode is generated for zero width`() = runTest {
        assertThat(underTest.createQRCode("hello", 0, 1000)).isNotNull()
    }

    @Test
    fun `test that a bitset result is generated for non-empty text`() = runTest {
        val expectedWidth = 1000
        val result = underTest.createQRCode("http://helloworld", expectedWidth, expectedWidth)
        assertThat(result).isNotNull()
        assertThat(result?.width).isEqualTo(expectedWidth)
        assertThat(result?.height).isEqualTo(expectedWidth)
        assertThat(result?.bits?.size()).isEqualTo(expectedWidth * expectedWidth)

        // Assert that there are "true" value in the generated bitset
        assertThat(result?.bits?.toByteArray()?.any { it != 0x00.toByte() }).isTrue()
    }

    @Test
    fun `test that a bitset result is generated when text is not empty and width does not equal to height`() =
        runTest {
            val expectedWidth = 1000
            val expectedHeight = 2000
            val result = underTest.createQRCode("http://helloworld", expectedWidth, expectedHeight)
            assertThat(result).isNotNull()
            assertThat(result?.width).isEqualTo(expectedWidth)
            assertThat(result?.height).isEqualTo(expectedHeight)
            assertThat(result?.bits?.size()).isEqualTo(expectedWidth * expectedHeight)

            // Assert that there are "true" value in the generated bitset
            assertThat(result?.bits?.toByteArray()?.any { it != 0x00.toByte() }).isTrue()
        }
}