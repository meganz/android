package mega.privacy.android.app.service.scanner

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.presentation.documentscanner.model.HandleScanDocumentResult
import mega.privacy.android.app.presentation.qrcode.model.BarcodeScanResult
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [ScannerHandler]
 */
class ScannerHandlerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ScannerHandler {

    /**
     * The ML Kit Barcode Scanner
     */
    private val barcodeScanner: GmsBarcodeScanner by lazy {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    override suspend fun scanBarcode() = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val moduleInstall = ModuleInstall.getClient(context)
            val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                .addApi(GmsBarcodeScanning.getClient(context))
                .build()
            moduleInstall
                .installModules(moduleInstallRequest)
                .addOnSuccessListener { moduleResponse ->
                    if (moduleResponse.areModulesAlreadyInstalled()) {
                        barcodeScanner.startScan()
                            .addOnSuccessListener {
                                continuation.resumeWith(Result.success(BarcodeScanResult.Success(it.rawValue)))
                            }
                            .addOnCanceledListener {
                                continuation.resumeWith(Result.success(BarcodeScanResult.Cancelled))
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

    override suspend fun handleScanDocument() = withContext(ioDispatcher) {
        if (getFeatureFlagValueUseCase(AppFeatures.DocumentScanner)) {
            buildDocumentScanner()
        } else {
            HandleScanDocumentResult.UseLegacyImplementation
        }
    }

    /**
     * When [AppFeatures.DocumentScanner] is enabled, this attempts to install the ML Kit Document
     * Scanner from Google Play services
     *
     * @return the ML Kit Document Scanner installation result. If successful, the Document Scanner
     * is returned for the caller's use
     */
    private suspend fun buildDocumentScanner(): HandleScanDocumentResult =
        suspendCancellableCoroutine { continuation ->
            val options = GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(10)
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
                        Timber.d("The ML Document Kit Scanner is successfully installed")
                        continuation.resumeWith(
                            Result.success(
                                HandleScanDocumentResult.UseNewImplementation(documentScanner)
                            )
                        )
                    } else {
                        Timber.e("The ML Document Kit Scanner is not installed")
                        continuation.resumeWith(Result.failure(DocumentScannerModuleIsNotInstalled()))
                    }
                }.addOnFailureListener {
                    Timber.e("An Exception occurred when installing the ML Document Kit Scanner:\n\n ${it.printStackTrace()}")
                    continuation.resumeWith(
                        Result.failure(
                            if (it is MlKitException && it.errorCode == MlKitException.UNSUPPORTED) {
                                InsufficientRAMToLaunchDocumentScanner()
                            } else {
                                UnexpectedErrorInDocumentScanner()
                            }
                        )
                    )
                }
        }
}

/**
 * An exception that should be thrown whenever the [GmsBarcodeScanning] module has not been installed.
 */
class BarcodeScannerModuleIsNotInstalled : Exception()

/**
 * An Exception that should be thrown whenever the [GmsDocumentScanning] module has not been installed
 */
class DocumentScannerModuleIsNotInstalled : Exception()

/**
 * An Exception that should be thrown when attempting to run the ML Document Scanner with less than
 * 1.7 GB Device total RAM
 */
class InsufficientRAMToLaunchDocumentScanner : Exception()

/**
 * An Exception that should be thrown when an unexpected error was found trying to install the
 * ML Document Kit Scanner
 */
class UnexpectedErrorInDocumentScanner : Exception()
