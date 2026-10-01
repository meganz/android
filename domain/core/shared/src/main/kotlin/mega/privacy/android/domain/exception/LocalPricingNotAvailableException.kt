package mega.privacy.android.domain.exception

/**
 * Exception thrown when local pricing from Google Play is not available for any subscription SKU,
 * typically because querySkus failed (e.g. debug build with different package name).
 */
class LocalPricingNotAvailableException :
    RuntimeException("Local pricing not available for any subscription SKU")
