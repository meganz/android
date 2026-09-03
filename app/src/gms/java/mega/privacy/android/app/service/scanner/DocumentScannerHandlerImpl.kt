package mega.privacy.android.app.service.scanner

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.core.nodecomponents.scanner.DocumentScannerHandler
import mega.privacy.android.core.nodecomponents.scanner.DocumentScannerModuleIsNotInstalled
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.UnexpectedErrorInDocumentScanner
import mega.privacy.android.domain.qualifier.IoDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [DocumentScannerHandler]
 */
class DocumentScannerHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DocumentScannerHandler {

    override suspend fun prepareDocumentScanner() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(30)
                .setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
                .setScannerMode(SCANNER_MODE_FULL)
                .build()
            val documentScanner = GmsDocumentScanning.getClient(options)

            val moduleInstall = ModuleInstall.getClient(context)
            val moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                    .addApi(documentScanner)
                    .build()
            moduleInstall
                .installModules(moduleInstallRequest)
                .addOnSuccessListener { moduleResponse ->
                    if (moduleResponse.areModulesAlreadyInstalled()) {
                        Timber.d("The ML Kit Document Scanner is present on the device")
                        continuation.resumeWith(Result.success(documentScanner))
                    } else {
                        Timber.e("The ML Kit Document Scanner is not present on the device")
                        continuation.resumeWith(Result.failure(DocumentScannerModuleIsNotInstalled()))
                    }
                }.addOnFailureListener {
                    val exception =
                        if (it is MlKitException && it.errorCode == MlKitException.UNSUPPORTED) {
                            InsufficientRAMToLaunchDocumentScanner()
                        } else {
                            UnexpectedErrorInDocumentScanner()
                        }

                    val errorInfo = if (it is MlKitException) {
                        "The error code is ${it.errorCode}, error message is ${it.message}"
                    } else {
                        "The exception is not MlKitException, message is ${it.message}"
                    }
                    Timber.e(
                        exception,
                        "An Exception occurred when installing the ML Kit Document Scanner, $errorInfo",
                    )
                    continuation.resumeWith(Result.failure(exception))
                }
        }
    }
}
