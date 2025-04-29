package mega.privacy.android.data.repository.files

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.shockwave.pdfium.PdfiumCore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.constant.FileConstant
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.files.PdfRepository
import timber.log.Timber
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Pdf repository impl.
 *
 * @property context
 * @property ioDispatcher
 */
class PdfRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApi: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileGateway: FileGateway,
) : PdfRepository {

    override suspend fun createThumbnail(nodeHandle: Long, uriPath: UriPath) =
        createThumbnailOrPreview(nodeHandle = nodeHandle, uriPath = uriPath, isPreview = false)

    override suspend fun createPreview(nodeHandle: Long, uriPath: UriPath) =
        createThumbnailOrPreview(nodeHandle = nodeHandle, uriPath = uriPath, isPreview = true)

    private suspend fun createThumbnailOrPreview(
        nodeHandle: Long,
        uriPath: UriPath,
        isPreview: Boolean,
    ) = withContext(ioDispatcher) {
        val pdfiumCore = PdfiumCore(context)
        val pageNumber = 0
        val fileName = megaApi.handleToBase64(nodeHandle) + FileConstant.JPG_EXTENSION
        val file = if (isPreview) {
            cacheGateway.getCacheFile(CacheFolderConstant.PREVIEW_FOLDER, fileName)
        } else {
            cacheGateway.getCacheFile(CacheFolderConstant.THUMBNAIL_FOLDER, fileName)
        } ?: return@withContext null

        val out = FileOutputStream(file)
        val pdfDocument = pdfiumCore.newDocument(
            fileGateway.getFileDescriptor(uriPath, false) ?: return@withContext null
        )

        try {
            pdfiumCore.openPage(pdfDocument, pageNumber)
            val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
            val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
            val bitmap = createBitmap(width, height)
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageNumber, 0, 0, width, height)
            val resizedBitmap = if (isPreview) {
                val resize = if (width > height) 1000f / width else 1000f / height
                val resizeWidth = (width * resize).toInt()
                val resizeHeight = (height * resize).toInt()
                bitmap.scale(resizeWidth, resizeHeight, false)
            } else {
                bitmap.scale(200, 200, false)
            }
            val result = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

            if (result) {
                Timber.d("Compress OK!")
                return@withContext file.absolutePath
            } else {
                Timber.w("Not Compress")
                return@withContext null
            }
        } finally {
            pdfiumCore.closeDocument(pdfDocument)
            out.close()
        }
    }
}