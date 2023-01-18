package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.QRCodeRepository
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [QRCodeRepository]
 */
class DefaultQRCodeRepository @Inject constructor(
    private val cacheFolderGateway: CacheFolderGateway,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : QRCodeRepository {

    override suspend fun getQRFile(fileName: String): File? = withContext(defaultDispatcher) {
        cacheFolderGateway.getCacheFile(CacheFolderConstant.QR_FOLDER, fileName)
    }
}