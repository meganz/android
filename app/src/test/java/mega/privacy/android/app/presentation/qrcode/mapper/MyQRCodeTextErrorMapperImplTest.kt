package mega.privacy.android.app.presentation.qrcode.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.QRCodeException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MyQRCodeTextErrorMapperImplTest {

    private lateinit var underTest: MyQRCodeTextErrorMapperImpl

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        underTest = MyQRCodeTextErrorMapperImpl(context = context)
    }

    @Test
    fun `test that MyQRCodeException ResetFailed maps to qrcode_reset_not_successfully`() =
        runTest {
            assertThat(
                underTest(
                    QRCodeException.ResetFailed(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.qrcode_reset_not_successfully))
        }

    @Test
    fun `test that MyQRCodeException DeleteFailed maps to qrcode_delete_not_successfully`() =
        runTest {
            assertThat(
                underTest(
                    QRCodeException.DeleteFailed(
                        1,
                        null
                    )
                )
            ).isEqualTo(context.getString(R.string.qrcode_delete_not_successfully))
        }

    @Test
    fun `test that MyQRCodeException CreateFailed maps to general_error`() = runTest {
        assertThat(
            underTest(
                QRCodeException.CreateFailed(
                    1, null
                )
            )
        ).isEqualTo(context.getString(R.string.general_error))
    }
}