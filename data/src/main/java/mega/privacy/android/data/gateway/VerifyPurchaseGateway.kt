package mega.privacy.android.data.gateway

internal interface VerifyPurchaseGateway {
    fun verifyPurchase(signedData: String, signature: String): Boolean
}