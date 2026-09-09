package mega.privacy.android.feature.contact.qrcode.mycode.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.exception.QRCodeException
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeError
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MyQrCodeErrorMapperTest {

    private val underTest = MyQrCodeErrorMapper()

    @Test
    fun `test that invoke returns ResetFailed when error is QRCodeException ResetFailed`() {
        assertThat(underTest(QRCodeException.ResetFailed(errorCode = 1)))
            .isEqualTo(MyQrCodeError.ResetFailed)
    }

    @Test
    fun `test that invoke returns DeleteFailed when error is QRCodeException DeleteFailed`() {
        assertThat(underTest(QRCodeException.DeleteFailed(errorCode = 1)))
            .isEqualTo(MyQrCodeError.DeleteFailed)
    }

    @Test
    fun `test that invoke returns General when error is an unrelated exception`() {
        assertThat(underTest(RuntimeException("boom"))).isEqualTo(MyQrCodeError.General)
    }
}
