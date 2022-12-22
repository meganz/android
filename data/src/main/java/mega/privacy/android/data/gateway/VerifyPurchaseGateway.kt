package mega.privacy.android.data.gateway

import java.io.IOException

internal interface VerifyPurchaseGateway {
    @Throws(IOException::class)
    fun verifyPurchase(signedData: String, signature: String): Boolean

    fun generateObfuscatedAccountId(): String?
}