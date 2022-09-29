package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CacheFileRepository
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default implementation of [CacheFileRepository]
 *
 * @property Context ApplicationContext
 * @property ioDispatcher CoroutineDispatcher
 */
class DefaultCacheFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CacheFileRepository {

    override fun purgeDirectory(directory: File) {
        Timber.d("Removing cache files")
        if (!directory.exists()) {
            return
        }
        try {
            directory.listFiles()?.let {
                for (file in it) {
                    if (file.isDirectory) {
                        purgeDirectory(file)
                    }
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }

    override suspend fun purgeCacheDirectory() = withContext(ioDispatcher) {
        purgeDirectory(File(context.cacheDir.toString() + Constants.SEPARATOR))
    }
}