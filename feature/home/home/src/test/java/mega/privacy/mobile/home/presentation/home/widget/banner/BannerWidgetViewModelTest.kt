package mega.privacy.mobile.home.presentation.home.widget.banner

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.banner.PromotionalBanner
import mega.privacy.android.domain.usecase.banner.DismissBannerUseCase
import mega.privacy.android.domain.usecase.banner.GetPromoBannersUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BannerWidgetViewModelTest {

    private lateinit var underTest: BannerWidgetViewModel
    private val getPromoBannersUseCase = mock<GetPromoBannersUseCase>()
    private val dismissBannerUseCase = mock<DismissBannerUseCase>()

    private val banner1 = PromotionalBanner(
        id = 1,
        title = "Test Banner 1",
        image = "image1.png",
        backgroundImage = "bg1.png",
        url = "https://test.com/1",
        imageLocation = "https://cdn.test.com/",
        buttonText = "Lear More"
    )

    private val banner2 = PromotionalBanner(
        id = 2,
        title = "Test Banner 2",
        image = "image2.png",
        backgroundImage = "bg2.png",
        url = "https://test.com/2",
        imageLocation = "https://cdn.test.com/",
        buttonText = "Lear More"
    )

    @BeforeEach
    fun setUp() {
        reset(dismissBannerUseCase, getPromoBannersUseCase)
    }

    private fun initViewModel() {
        underTest = BannerWidgetViewModel(
            dismissBannerUseCase = dismissBannerUseCase,
            getPromoBannersUseCase = getPromoBannersUseCase
        )
    }

    @Test
    fun `test that empty list is returned when no banners available`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(emptyList())

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that banners are loaded successfully`() = runTest {
        val bannerList = listOf(banner1, banner2)
        whenever(getPromoBannersUseCase()).thenReturn(bannerList)

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).containsExactly(banner1, banner2).inOrder()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that single banner is loaded successfully`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).containsExactly(banner1)
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that empty list is returned when loading fails`() = runTest {
        whenever(getPromoBannersUseCase()).thenThrow(RuntimeException("Network error"))

        initViewModel()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that dismissBanner calls use case`() = runTest {
        val bannerList = listOf(banner1, banner2)
        whenever(getPromoBannersUseCase()).thenReturn(bannerList)
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.dismissBanner(1)

        verify(dismissBannerUseCase).invoke(1)
    }

    @Test
    fun `test that dismissBanner removes banner from list`() = runTest {
        val bannerList = listOf(banner1, banner2)
        whenever(getPromoBannersUseCase()).thenReturn(bannerList)
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.uiState.test {
            // Skip initial state
            awaitItem()

            underTest.dismissBanner(1)

            val state = awaitItem()
            assertThat(state.banners).containsExactly(banner2)
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that dismissing last banner results in empty list`() = runTest {
        whenever(getPromoBannersUseCase()).thenReturn(listOf(banner1))
        whenever(dismissBannerUseCase(1)).thenReturn(Unit)

        initViewModel()

        underTest.uiState.test {
            // Skip initial state
            awaitItem()

            underTest.dismissBanner(1)

            val state = awaitItem()
            assertThat(state.banners).isEmpty()
            assertThat(state.isLoading).isFalse()
        }
    }

    @Test
    fun `test that dismissBanner failure keeps current list`() = runTest {
        val bannerList = listOf(banner1, banner2)
        whenever(getPromoBannersUseCase()).thenReturn(bannerList)
        whenever(dismissBannerUseCase(1)).thenThrow(RuntimeException("Dismiss failed"))

        initViewModel()

        underTest.uiState.test {
            val initialState = awaitItem()

            underTest.dismissBanner(1)

            // State should remain unchanged
            expectNoEvents()
            assertThat(underTest.uiState.value).isEqualTo(initialState)
        }
    }
}
