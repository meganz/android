package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCookieDialogUseCaseTest {

    private lateinit var underTest: GetCookieDialogUseCase
    private val shouldShowCookieDialogWithAdsUseCase = mock<ShouldShowCookieDialogWithAdsUseCase>()
    private val shouldShowGenericCookieDialogUseCase = mock<ShouldShowGenericCookieDialogUseCase>()
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()
    private val getSessionTransferURLUseCase = mock<GetSessionTransferURLUseCase>()

    private val url = "https://mega.nz/cookie"

    @BeforeEach
    fun setUp() {
        underTest = GetCookieDialogUseCase(
            shouldShowCookieDialogWithAdsUseCase,
            shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            getSessionTransferURLUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            shouldShowCookieDialogWithAdsUseCase,
            shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            getSessionTransferURLUseCase,
        )
    }

    @Test
    fun `test that CookieDialogWithAds type is returned when shouldShowCookieDialogWithAdsUseCase returns true`() =
        runTest {
            whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
            whenever(getSessionTransferURLUseCase(any())).thenReturn(url)
            whenever(updateCookieSettingsUseCase(any())).thenReturn(Unit)
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(true)

            val result = underTest(
                mock(),
                mock(),
                mock()
            )
            val expected = CookieDialog(
                CookieDialogType.CookieDialogWithAds,
                url
            )

            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that GenericCookieDialog type is returned when shouldShowCookieDialogWithAdsUseCase returns false and shouldShowGenericCookieDialogUseCase returns true`() =
        runTest {
            whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
            whenever(getSessionTransferURLUseCase(any())).thenReturn(url)
            whenever(updateCookieSettingsUseCase(any())).thenReturn(Unit)
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(false)
            whenever(shouldShowGenericCookieDialogUseCase(any())).thenReturn(true)

            val result = underTest(
                mock(),
                mock(),
                mock()
            )
            val expected = CookieDialog(
                CookieDialogType.GenericCookieDialog,
                url
            )

            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that None type is returned when shouldShowCookieDialogWithAdsUseCase and shouldShowGenericCookieDialogUseCase return false`() =
        runTest {
            whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
            whenever(getSessionTransferURLUseCase(any())).thenReturn(null)
            whenever(updateCookieSettingsUseCase(any())).thenReturn(Unit)
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(false)
            whenever(shouldShowGenericCookieDialogUseCase(any())).thenReturn(false)

            val result = underTest(
                mock(),
                mock(),
                mock()
            )
            val expected = CookieDialog(
                CookieDialogType.None,
            )
            assertThat(result).isEqualTo(expected)
        }
}