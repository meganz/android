package mega.privacy.android.app.appstate.global.initialisation.appcreate

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.fetcher.MegaImageLoaderFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(DelicateCoilApi::class)
class CoilImageLoaderInitialiserTest {

    private val megaImageLoaderFactory = mock<MegaImageLoaderFactory>()
    private val underTest = CoilImageLoaderInitialiser(megaImageLoaderFactory)

    @BeforeEach
    fun setUp() {
        SingletonImageLoader.reset()
    }

    @AfterEach
    fun tearDown() {
        SingletonImageLoader.reset()
    }

    @Test
    fun `test that invoke installs the factory as the singleton image loader factory`() {
        val imageLoader = mock<ImageLoader>()
        val context = mock<Context>()
        whenever(context.applicationContext).thenReturn(context)
        whenever(megaImageLoaderFactory.newImageLoader(any())).thenReturn(imageLoader)

        underTest()

        assertThat(SingletonImageLoader.get(context)).isSameInstanceAs(imageLoader)
    }
}
