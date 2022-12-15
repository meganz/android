package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.QRCodeGateway
import mega.privacy.android.data.model.QRCodeBitSet
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.BitSet

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultQRCodeRepositoryTest {

    private lateinit var underTest: DefaultQRCodeRepository
    private val qrCodeGateway = mock<QRCodeGateway>()
    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val penColor = 0x121212
    private val bgColor = 0xFFFFFF
    private val width = 100
    private val height = 2000

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
        underTest = DefaultQRCodeRepository(qrCodeGateway, testCoroutineDispatcher)
    }

    @Test
    fun `test that empty text generates null QRCode bitmap`() = runTest {
        whenever(qrCodeGateway.createQRCode(any(), any(), any())).thenReturn(null)
        assertThat(underTest.createQRCode(
            text = "",
            width = width,
            height = width,
            color = penColor,
            backgroundColor = bgColor)).isNull()
    }

    @Test
    fun `test that null bit set in QRCodeBitset generates null QRCode bitmap`() = runTest {
        whenever(qrCodeGateway.createQRCode(any(), any(), any()))
            .thenReturn(
                QRCodeBitSet(
                    width = width,
                    height = width,
                    bits = null))
        assertThat(underTest.createQRCode(
            text = "http://hello",
            width = width,
            height = width,
            color = penColor,
            backgroundColor = bgColor)
        ).isNull()
    }

    @Test
    fun `test that QRCodeBitmap is generated when QRCodeBitSet is not null and width equals to height`() =
        runTest {
            val bits = BitSet(width * width)
            for (index in 0 until width * width) {
                bits[index] = index % 2 == 0
            }

            whenever(qrCodeGateway.createQRCode(any(), any(), any()))
                .thenReturn(
                    QRCodeBitSet(
                        width = width,
                        height = width,
                        bits = bits)
                )
            val bitmap = underTest.createQRCode(text = "http://hello",
                width = width,
                height = width,
                color = penColor,
                backgroundColor = bgColor)

            assertThat(bitmap).isNotNull()
            assertThat(bitmap?.width).isEqualTo(width)
            assertThat(bitmap?.height).isEqualTo(width)
            assertThat(bitmap?.pixels).isNotNull()
            assertThat(bitmap?.pixels?.size).isEqualTo(width * width)

            for (index in 0 until width * width) {
                val expectedPixel: Int = if (bits[index]) penColor else bgColor
                val pixel = bitmap?.pixels?.get(index)
                assertThat(pixel).isEqualTo(expectedPixel)
            }
        }

    @Test
    fun `test that QRCodeBitmap is generated when QRCodeBitSet is not null and width does not equal to height`() =
        runTest {
            val bits = BitSet(width * height)
            for (index in 0 until width * height) {
                bits[index] = index % 2 == 0
            }

            whenever(qrCodeGateway.createQRCode(any(), any(), any()))
                .thenReturn(
                    QRCodeBitSet(
                        width = width,
                        height = height,
                        bits = bits)
                )
            val bitmap = underTest.createQRCode(text = "http://hello",
                width = width,
                height = height,
                color = penColor,
                backgroundColor = bgColor)

            assertThat(bitmap).isNotNull()
            assertThat(bitmap?.width).isEqualTo(width)
            assertThat(bitmap?.height).isEqualTo(height)
            assertThat(bitmap?.pixels).isNotNull()
            assertThat(bitmap?.pixels?.size).isEqualTo(width * height)

            for (index in 0 until width * height) {
                val expectedPixel: Int = if (bits[index]) penColor else bgColor
                val pixel = bitmap?.pixels?.get(index)
                assertThat(pixel).isEqualTo(expectedPixel)
            }
        }

}