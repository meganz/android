package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
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
internal class GetCookieDialogTypeUseCaseTest {

    private lateinit var underTest: GetCookieDialogTypeUseCase
    private val shouldShowCookieDialogWithAdsUseCase = mock<ShouldShowCookieDialogWithAdsUseCase>()
    private val shouldShowGenericCookieDialogUseCase = mock<ShouldShowGenericCookieDialogUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = GetCookieDialogTypeUseCase(
            shouldShowCookieDialogWithAdsUseCase,
            shouldShowGenericCookieDialogUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            shouldShowCookieDialogWithAdsUseCase,
            shouldShowGenericCookieDialogUseCase,
        )
    }

    @Test
    fun `Test that Cookie dialog type should be CookieDialogWithAds when shouldShowCookieDialogWithAdsUseCase return true`() =
        runTest {
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
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

            assert(result == CookieDialogType.CookieDialogWithAds)
        }

    @Test
    fun `Test that Cookie dialog type should be GenericCookieDialog when shouldShowCookieDialogWithAdsUseCase return false and shouldShowGenericCookieDialogUseCase return true`() =
        runTest {
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(false)
            whenever(shouldShowGenericCookieDialogUseCase()).thenReturn(true)

            val result = underTest(
                mock(),
                mock(),
                mock()
            )

            assert(result == CookieDialogType.GenericCookieDialog)
        }

    @Test
    fun `Test that Cookie dialog type should be None when shouldShowCookieDialogWithAdsUseCase and shouldShowGenericCookieDialogUseCase return false`() =
        runTest {
            whenever(
                shouldShowCookieDialogWithAdsUseCase(
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(false)
            whenever(shouldShowGenericCookieDialogUseCase()).thenReturn(false)

            val result = underTest(
                mock(),
                mock(),
                mock()
            )
            assert(result == CookieDialogType.None)
        }
}