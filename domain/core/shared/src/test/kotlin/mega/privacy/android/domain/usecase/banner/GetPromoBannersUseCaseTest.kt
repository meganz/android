package mega.privacy.android.domain.usecase.banner

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.entity.banner.PromotionalBanner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetPromoBannersUseCaseTest {

    private lateinit var underTest: GetPromoBannersUseCase
    private val getBannersUseCase: GetBannersUseCase = mock()

    private val testBanners = listOf(
        Banner(
            id = 1,
            title = "Test Banner",
            description = "Test Description",
            image = "banner1.jpg",
            backgroundImage = "bg1.jpg",
            imageLocation = "https://images.example.com",
            url = "https://test.com",
            buttonText = "Click Me",
            variant = 1
        ),
        Banner(
            id = 2,
            title = "Test Banner 2",
            description = "Test Description 2",
            image = "banner2.jpg",
            backgroundImage = "bg2.jpg",
            imageLocation = "https://images.example.com",
            url = "https://test2.com",
            buttonText = null,
            variant = 1
        )
    )

    @BeforeAll
    fun setUp() {
        underTest = GetPromoBannersUseCase(getBannersUseCase)
    }

    @AfterEach
    fun clear() {
        reset(getBannersUseCase)
    }

    @Test
    fun `test that invoke returns promotional banners with correct mapping`() = runTest {
        whenever(getBannersUseCase()).thenReturn(testBanners)

        val actual = underTest()

        val expected = listOf(
            PromotionalBanner(
                id = 1,
                title = "Test Banner",
                buttonText = "Click Me",
                image = "https://images.example.com/banner1.jpg",
                backgroundImage = "https://images.example.com/bg1.jpg",
                url = "https://test.com",
                imageLocation = "https://images.example.com"
            ),
            PromotionalBanner(
                id = 2,
                title = "Test Banner 2",
                buttonText = "",
                image = "https://images.example.com/banner2.jpg",
                backgroundImage = "https://images.example.com/bg2.jpg",
                url = "https://test2.com",
                imageLocation = "https://images.example.com"
            )
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns empty list when no banners available`() = runTest {
        whenever(getBannersUseCase()).thenReturn(emptyList())

        val actual = underTest()

        assertThat(actual).isEmpty()
    }

    @Test
    fun `test that invoke filters banners by variant 1 only`() = runTest {
        val bannersWithDifferentVariants = listOf(
            Banner(
                id = 1,
                title = "Variant 1 Banner",
                description = "Description",
                image = "banner1.jpg",
                backgroundImage = "bg1.jpg",
                imageLocation = "https://images.example.com",
                url = "https://test1.com",
                buttonText = "Click Me",
                variant = 1
            ),
            Banner(
                id = 2,
                title = "Variant 2 Banner",
                description = "Description",
                image = "banner2.jpg",
                backgroundImage = "bg2.jpg",
                imageLocation = "https://images.example.com",
                url = "https://test2.com",
                buttonText = "Click Me",
                variant = 2
            ),
            Banner(
                id = 3,
                title = "Variant 0 Banner",
                description = "Description",
                image = "banner3.jpg",
                backgroundImage = "bg3.jpg",
                imageLocation = "https://images.example.com",
                url = "https://test3.com",
                buttonText = "Click Me",
                variant = 0
            )
        )

        whenever(getBannersUseCase()).thenReturn(bannersWithDifferentVariants)

        val actual = underTest()

        assertThat(actual).hasSize(1)
        assertThat(actual[0].id).isEqualTo(1)
        assertThat(actual[0].title).isEqualTo("Variant 1 Banner")
    }

    @Test
    fun `test that invoke constructs image URLs correctly`() = runTest {
        val banner = listOf(
            Banner(
                id = 1,
                title = "Test Banner",
                description = "Description",
                image = "banner.jpg",
                backgroundImage = "background.jpg",
                imageLocation = "https://cdn.example.com/images",
                url = "https://test.com",
                buttonText = "Test",
                variant = 1
            )
        )

        whenever(getBannersUseCase()).thenReturn(banner)

        val actual = underTest()
        assertThat(actual[0].image).isEqualTo("https://cdn.example.com/images/banner.jpg")
        assertThat(actual[0].backgroundImage).isEqualTo("https://cdn.example.com/images/background.jpg")
        assertThat(actual[0].imageLocation).isEqualTo("https://cdn.example.com/images")
    }
}
