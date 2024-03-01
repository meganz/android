package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.FileReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

/**
 * Use case to get path from [NodeContentUri]
 * @property ioDispatcher [CoroutineDispatcher]
 */
class GetPathFromNodeContentUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * invoke
     * @param content [NodeContentUri]
     * @return path
     */
    suspend operator fun invoke(content: NodeContentUri): String? {
        return withContext(ioDispatcher) {
            val br = runCatching {
                when (content) {
                    is NodeContentUri.LocalContentUri -> {
                        FileReader(content.file).buffered()
                    }

                    is NodeContentUri.RemoteContentUri -> {
                        val url = URL(content.url)
                        val connection = url.openConnection() as? HttpURLConnection
                        connection?.inputStream?.bufferedReader()
                    }
                }
            }.getOrNull()
            val path = br?.let {
                var line = it.readLine()
                line?.let { _ ->
                    line = it.readLine()
                    line.replace("URL=", "")
                }
            }
            br?.close()
            path
        }
    }
}