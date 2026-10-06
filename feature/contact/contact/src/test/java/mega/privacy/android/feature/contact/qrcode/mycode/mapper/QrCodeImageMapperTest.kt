package mega.privacy.android.feature.contact.qrcode.mycode.mapper

import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.shared.account.presentation.model.PhotoAvatarContent
import mega.privacy.android.shared.account.presentation.model.TextAvatarContent
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class QrCodeImageMapperTest {

    private lateinit var underTest: QrCodeImageMapper

    private val textAvatar = TextAvatarContent(
        avatarText = "A",
        backgroundColor = 0xFFF30C14.toInt(),
        showBorder = true,
        textSize = 38.sp,
    )

    @Before
    fun setUp() {
        underTest = QrCodeImageMapper(
            context = ApplicationProvider.getApplicationContext(),
            defaultDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that invoke returns an encoded image for a text avatar`() = runTest {
        val result = underTest("https://mega.nz/C!abcdef", textAvatar)

        assertThat(result).isNotNull()
        assertThat(result).isNotEmpty()
    }

    @Test
    fun `test that invoke returns an encoded image when the photo avatar cannot be decoded`() =
        runTest {
            val photoAvatar = PhotoAvatarContent(
                path = "file:///missing/avatar.jpg",
                size = 1L,
                showBorder = true,
            )

            val result = underTest("https://mega.nz/C!abcdef", photoAvatar)

            assertThat(result).isNotNull()
        }

    @Test
    fun `test that invoke returns null when the contact link is blank`() = runTest {
        assertThat(underTest(" ", textAvatar)).isNull()
    }
}
