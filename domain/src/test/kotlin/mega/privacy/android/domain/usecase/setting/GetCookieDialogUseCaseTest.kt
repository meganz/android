package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
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
    private val shouldShowGenericCookieDialogUseCase = mock<ShouldShowGenericCookieDialogUseCase>()
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()

    private val url = "https://mega.nz/cookie"

    @BeforeEach
    fun setUp() {
        underTest = GetCookieDialogUseCase(
            shouldShowGenericCookieDialogUseCase = shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase = getCookieSettingsUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase,
        )
    }

    @Test
    fun `test that GenericCookieDialog type is returned when shouldShowGenericCookieDialogUseCase returns true`() =
        runTest {
            whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
            whenever(shouldShowGenericCookieDialogUseCase(any())).thenReturn(true)

            val result = underTest()
            val expected = CookieDialog(
                CookieDialogType.GenericCookieDialog,
                url
            )

            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that None type is returned when shouldShowGenericCookieDialogUseCase return false`() =
        runTest {
            whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
            whenever(shouldShowGenericCookieDialogUseCase(any())).thenReturn(false)

            val result = underTest()
            val expected = CookieDialog(
                CookieDialogType.None,
            )
            assertThat(result).isEqualTo(expected)
        }
}