package mega.privacy.android.domain.usecase.setting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.settings.cookie.CookieDialog
import mega.privacy.android.domain.entity.settings.cookie.CookieDialogType
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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
    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetCookieDialogUseCase(
            shouldShowGenericCookieDialogUseCase = shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            getDomainNameUseCase = getDomainNameUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            shouldShowGenericCookieDialogUseCase,
            getCookieSettingsUseCase,
            getDomainNameUseCase,
        )
    }

    @ParameterizedTest
    @MethodSource("domainProvider")
    fun `test that GenericCookieDialog type is returned when shouldShowGenericCookieDialogUseCase returns true`(
        domain: String,
    ) = runTest {
        val url = "https://$domain/cookie"
        whenever(getCookieSettingsUseCase()).thenReturn(emptySet())
        whenever(shouldShowGenericCookieDialogUseCase(any())).thenReturn(true)
        whenever(getDomainNameUseCase()).thenReturn(domain)

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

    private fun domainProvider() = listOf("mega.nz", "mega.app")
}