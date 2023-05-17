package mega.privacy.android.app.initializer.fresco

import android.util.SparseIntArray
import com.facebook.common.util.ByteConstants
import com.facebook.imagepipeline.memory.PoolParams

/**
 * Length of 'small' sized buckets. Bucket lengths for these buckets are larger because they're
 * smaller in size
 */
private const val SMALL_BUCKET_LENGTH = 5

/** Bucket lengths for 'large' (> 256KB) buckets  */
private const val LARGE_BUCKET_LENGTH = 2

/**
 * Provides pool parameters ([PoolParams]) for NativeMemoryChunkPool
 * @param availableMemory the available system memory, usually obtained with the extension [context.getAvailableMemory()]
 * @param maxMemory the maximum amount of memory that the Java virtual machine will
 * attempt to use, usually obtained with [Runtime.getRuntime().maxMemory()]
 */
internal fun getFrescoNativeMemoryChunkPoolParams(
    availableMemory: Long,
    maxMemory: Long,
): PoolParams {
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
    val maxSizeCap = getMaxSizeCapPair(availableMemory, maxMemory)

    return PoolParams(maxSizeCap.soft, maxSizeCap.hard, bucketSizes)
}

internal data class MaxSizeCap(val soft: Int, val hard: Int)

/**
 * Gets soft and hard max size caps to be used for creating Fresco's [PoolParams] given the available and max memory
 * This method ensures that the PoolParams needed preconditions are followed:
 * Preconditions.checkState(maxSizeSoftCap >= 0 && maxSizeHardCap >= maxSizeSoftCap);
 */
internal fun getMaxSizeCapPair(
    availableMemory: Long,
    maxMemory: Long,
): MaxSizeCap {
    val maxSizeHardCap = getMaxSizeHardCap(availableMemory)
    val maxSizeSoftCap = getMaxSizeSoftCap(maxMemory).coerceAtMost(maxSizeHardCap)
    return MaxSizeCap(maxSizeSoftCap, maxSizeHardCap)
}

/**
 * NativeMemoryChunkPool manages memory on the native heap, so we don't need as strict
 * caps as we would if we were on the Dalvik heap. However, since native memory OOMs are
 * significantly more problematic than Dalvik OOMs, we would like to stay conservative.
 * @param maxMemory the maximum amount of memory that the Java virtual machine will
 * attempt to use, usually obtained with [Runtime.getRuntime().maxMemory()]
 */
private fun getMaxSizeSoftCap(maxMemory: Long): Int {
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
 * @param availableMemory the available system memory, usually obtained with the extension [context.getAvailableMemory()]
 */
private fun getMaxSizeHardCap(availableMemory: Long): Int {
    val hardCap = if (availableMemory < 16 * ByteConstants.MB) {
        availableMemory / 2
    } else {
        availableMemory / 4 * 3
    }
    return hardCap.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
}
