package mega.privacy.android.core.nodecomponents.scanner

/**
 * An exception that should be thrown whenever the [com.google.mlkit.vision.codescanner.GmsBarcodeScanning] module has not been installed.
 */
class BarcodeScannerModuleIsNotInstalled : Exception()

/**
 * An Exception that should be thrown whenever the [com.google.mlkit.vision.documentscanner.GmsDocumentScanning] module has not been installed
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