package mega.privacy.android.app.utils

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants
import com.facebook.imagepipeline.memory.PoolParams
import kotlin.math.min

/** Provides pool parameters ([PoolParams]) for NativeMemoryChunkPool  */
object FrescoNativeMemoryChunkPoolParams {
    /**
     * Length of 'small' sized buckets. Bucket lengths for these buckets are larger because they're
     * smaller in size
     */
    private const val SMALL_BUCKET_LENGTH = 5

    /** Bucket lengths for 'large' (> 256KB) buckets  */
    private const val LARGE_BUCKET_LENGTH = 2

    @JvmStatic
    fun get(maxMemory: Long): PoolParams {
        val bucketSizes = SparseIntArray().apply {
            put(1 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(2 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(4 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(8 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(16 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(32 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(64 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(128 * ByteConstants.KB, SMALL_BUCKET_LENGTH)
            put(256 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
            put(512 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
            put(1024 * ByteConstants.KB, LARGE_BUCKET_LENGTH)
        }
        return PoolParams(getMaxSizeSoftCap(), getMaxSizeHardCap(maxMemory), bucketSizes)
    }

    /**
     * NativeMemoryChunkPool manages memory on the native heap, so we don't need as strict
     * caps as we would if we were on the Dalvik heap. However, since native memory OOMs are
     * significantly more problematic than Dalvik OOMs, we would like to stay conservative.
     */
    private fun getMaxSizeSoftCap(): Int {
        val maxMemory = min(Runtime.getRuntime().maxMemory(), Int.MAX_VALUE.toLong()).toInt()
        return when {
            maxMemory < 16 * ByteConstants.MB -> {
                3 * ByteConstants.MB
            }
            maxMemory < 32 * ByteConstants.MB -> {
                6 * ByteConstants.MB
            }
            else -> {
                12 * ByteConstants.MB
            }
        }
    }

    /**
     * We need a smaller cap for devices with less then 16 MB so that we don't run the risk of
     * evicting other processes from the native heap.
     */
    private fun getMaxSizeHardCap(maxMemory: Long): Int {
        val hardCap = if (maxMemory < 16 * ByteConstants.MB) {
            maxMemory / 2
        } else {
            maxMemory / 4 * 3
        }
        return hardCap.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}
