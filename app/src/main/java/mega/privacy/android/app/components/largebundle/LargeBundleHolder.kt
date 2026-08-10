package mega.privacy.android.app.components.largebundle

import android.content.Context
import android.os.Bundle
import android.os.Parcel
import androidx.collection.LruCache
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.FileSystemRepository
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds large [Bundle] payloads outside the Intent/SavedState path to avoid
 * [android.os.TransactionTooLargeException]. Memory-backed with an async disk fallback.
 *
 * **Before reaching for this, prefer cheaper alternatives:** re-load the data
 * from a repository on the receiving side, or pass a small identifier (parent id,
 * query, node handle) the consumer can resolve. Those approaches keep the binder
 * payload tiny and avoid the disk I/O incurred here. Use this holder when the
 * payload genuinely has to travel between Activities and re-fetching is not an option.
 */
@Deprecated("Interim solution. Prefer passing small identifiers and re-loading data from a repository.")
@Singleton
class LargeBundleHolder @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val mem = LruCache<String, Bundle>(16)

    /**
     * Store [bundle] and return a key.
     */
    fun put(bundle: Bundle): String {
        val key = UUID.randomUUID().toString()
        mem.put(key, bundle)
        appScope.launch(ioDispatcher) {
            runCatching {
                val bytes = bundle.marshallToBytes() ?: return@launch
                fileSystemRepository.saveLargeBundle(key, bytes)
            }.onFailure { Timber.e(it, "LargeBundleHolder: disk write failed for $key") }
        }
        return key
    }

    /**
     * Retrieve the Bundle for [key], falling back to disk if evicted from memory.
     */
    suspend fun get(key: String): Bundle? {
        mem[key]?.let { return it }
        return withContext(ioDispatcher) {
            runCatching {
                val bytes = fileSystemRepository.readLargeBundle(key) ?: return@runCatching null
                bytes.unmarshallToBundle()?.also { mem.put(key, it) }
            }.onFailure { Timber.e(it, "LargeBundleHolder: disk read failed for $key") }
                .getOrNull()
        }
    }

    /**
     * Release memory and disk copies for [key].
     */
    fun release(key: String) {
        mem.remove(key)
        appScope.launch(ioDispatcher) {
            runCatching {
                fileSystemRepository.deleteLargeBundle(key)
            }.onFailure { Timber.e(it, "LargeBundleHolder: disk delete failed for $key") }
        }
    }
}

private fun Bundle.marshallToBytes(): ByteArray? {
    val parcel = Parcel.obtain()
    return try {
        parcel.writeBundle(this)
        parcel.marshall()
    } catch (e: Exception) {
        Timber.e(e, "LargeBundleHolder: marshall failed")
        null
    } finally {
        parcel.recycle()
    }
}

private fun ByteArray.unmarshallToBundle(): Bundle? {
    val parcel = Parcel.obtain()
    return try {
        parcel.unmarshall(this, 0, this.size)
        parcel.setDataPosition(0)
        parcel.readBundle(LargeBundleHolder::class.java.classLoader)
    } catch (e: Exception) {
        Timber.e(e, "LargeBundleHolder: unmarshall failed")
        null
    } finally {
        parcel.recycle()
    }
}

/**
 * Extension to access [LargeBundleHolder] from non-Hilt call sites.
 */
val Context.largeBundleHolder: LargeBundleHolder
    get() = LargeBundleHolderProvider.get(this)

/**
 * Hilt entry point for [LargeBundleHolder].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LargeBundleHolderEntryPoint {
    val largeBundleHolder: LargeBundleHolder
}

internal object LargeBundleHolderProvider {
    private val ref = AtomicReference<LargeBundleHolder?>(null)
    fun get(context: Context): LargeBundleHolder = ref.get() ?: run {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LargeBundleHolderEntryPoint::class.java,
        ).largeBundleHolder.also { ref.set(it) }
    }
}
