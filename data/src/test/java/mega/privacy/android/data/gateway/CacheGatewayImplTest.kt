package mega.privacy.android.data.gateway

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheGatewayImplTest {

    private lateinit var underTest: CacheGatewayImpl

    private val context: Context = mock()
    private val deviceGateway: DeviceGateway = mock()

    private var nowNanos = 0L

    @TempDir
    lateinit var cacheDir: File

    private val thumbnailFolder: File
        get() = File(cacheDir, CacheFolderConstant.THUMBNAIL_FOLDER)

    @BeforeEach
    fun setUp() {
        nowNanos = 0L
        whenever(context.cacheDir).thenReturn(cacheDir)
        whenever(deviceGateway.nanoTime).doAnswer { nowNanos }
        underTest = CacheGatewayImpl(context, UnconfinedTestDispatcher(), deviceGateway)
    }

    private fun advanceTimeByRevalidateInterval() {
        nowNanos += CacheGatewayImpl.REVALIDATE_INTERVAL.inWholeNanoseconds
    }

    @Test
    fun `test that getThumbnailCacheFolderPath creates the folder and returns its path`() =
        runTest {
            val actual = underTest.getThumbnailCacheFolderPath()

            assertThat(actual).isEqualTo(thumbnailFolder.path)
            assertThat(thumbnailFolder.exists()).isTrue()
        }

    @Test
    fun `test that cached path is returned without folder check within revalidation interval`() =
        runTest {
            underTest.getThumbnailCacheFolderPath()
            thumbnailFolder.delete()

            val actual = underTest.getThumbnailCacheFolderPath()

            assertThat(actual).isEqualTo(thumbnailFolder.path)
            assertThat(thumbnailFolder.exists()).isFalse()
        }

    @Test
    fun `test that deleted folder is recreated when revalidation interval has passed`() =
        runTest {
            underTest.getThumbnailCacheFolderPath()
            thumbnailFolder.delete()
            advanceTimeByRevalidateInterval()

            val actual = underTest.getThumbnailCacheFolderPath()

            assertThat(actual).isEqualTo(thumbnailFolder.path)
            assertThat(thumbnailFolder.exists()).isTrue()
        }

    @Test
    fun `test that deleted folder is recreated when path cache is cleared`() = runTest {
        underTest.getThumbnailCacheFolderPath()
        thumbnailFolder.delete()
        underTest.clearPathCache()

        val actual = underTest.getThumbnailCacheFolderPath()

        assertThat(actual).isEqualTo(thumbnailFolder.path)
        assertThat(thumbnailFolder.exists()).isTrue()
    }
}
