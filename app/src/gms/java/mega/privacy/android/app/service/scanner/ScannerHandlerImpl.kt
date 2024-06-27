package mega.privacy.android.app.service.scanner

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.presentation.qrcode.model.ScanResult
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Implementation of scanner handler
 */
class ScannerHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ScannerHandler {
    private val scanner: GmsBarcodeScanner by lazy {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    override suspend fun scan() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val moduleInstall = ModuleInstall.getClient(context)
            val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                .addApi(GmsBarcodeScanning.getClient(context))
                .build()
            moduleInstall
                .installModules(moduleInstallRequest)
                .addOnSuccessListener { moduleResponse ->
                    if (moduleResponse.areModulesAlreadyInstalled()) {
                        scanner.startScan()
                            .addOnSuccessListener {
                                continuation.resumeWith(Result.success(ScanResult.Success(it.rawValue)))
                            }
                            .addOnCanceledListener {
                                continuation.resumeWith(Result.success(ScanResult.Cancel))
                            }
                            .addOnFailureListener {
                                continuation.resumeWith(Result.failure(it))
                            }
                    } else {
                        continuation.resumeWith(Result.failure(BarcodeScannerModuleIsNotInstalled()))
                    }
                }
                .addOnFailureListener {
                    continuation.resumeWith(Result.failure(it))
                }
        }
    }
}

/**
 * An exception that should be thrown whenever the [GmsBarcodeScanning] module has not been installed.
 */
class BarcodeScannerModuleIsNotInstalled : Exception()
