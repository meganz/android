package mega.privacy.android.data.facade

import android.util.Base64
import mega.privacy.android.data.gateway.VerifyPurchaseGateway
import timber.log.Timber
import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject

internal class VerifyPurchaseFacade @Inject constructor() : VerifyPurchaseGateway {
    /**
     * Verifies that the data was signed with the given signature, and returns the verified
     * purchase.
     *
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature  the signature for the data, signed with the private key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    @Throws(IOException::class)
    override fun verifyPurchase(signedData: String, signature: String): Boolean {
        if (signedData.isEmpty() || signature.isEmpty()) {
            Timber.w("Purchase verification failed: missing data.")
            return false
        }
        val key = generatePublicKey()
        return verify(key, signedData, signature)
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    @Throws(IOException::class)
    private fun generatePublicKey(): PublicKey {
        return try {
            val decodedKey = Base64.decode(PUBLIC_KEY, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            Timber.e(e, "RSA is unavailable.")
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            val msg = "Invalid key specification: $e"
            Timber.w(e, msg)
            throw IOException(msg)
        } catch (e: IllegalArgumentException) {
            Timber.w(e)
            throw IOException(e.message)
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey  public key associated with the developer account
     * @param signedData signed data from server
     * @param signature  server signature
     * @return true if the data and signature match
     */
    private fun verify(publicKey: PublicKey?, signedData: String, signature: String?): Boolean {
        val signatureBytes: ByteArray = try {
            Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Timber.w(e, "Base64 decoding failed.")
            return false
        }
        try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            if (!signatureAlgorithm.verify(signatureBytes)) {
                Timber.w("Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            // "RSA" is guaranteed to be available.
            Timber.e(e, "RSA is unavailable.")
            throw RuntimeException(e)
        } catch (e: InvalidKeyException) {
            Timber.w(e, "Invalid key specification.")
        } catch (e: SignatureException) {
            Timber.w(e, "Signature exception.")
        }
        return false
    }

    companion object {
        private const val KEY_FACTORY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

        private const val BASE64_ENCODED_PUBLIC_KEY_1 =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0bZjbgdGRd6/hw5/J2FGTkdG"
        private const val BASE64_ENCODED_PUBLIC_KEY_2 =
            "tDTMdR78hXKmrxCyZUEvQlE/DJUR9a/2ZWOSOoaFfi9XTBSzxrJCIa+gjj5wkyIwIrzEi"
        private const val BASE64_ENCODED_PUBLIC_KEY_3 =
            "55k9FIh3vDXXTHJn4oM9JwFwbcZf1zmVLyes5ld7+G15SZ7QmCchqfY4N/a/qVcGFsfwqm"
        private const val BASE64_ENCODED_PUBLIC_KEY_4 =
            "RU3VzOUwAYHb4mV/frPctPIRlJbzwCXpe3/mrcsAP+k6ECcd19uIUCPibXhsTkNbAk8CRkZ"
        private const val BASE64_ENCODED_PUBLIC_KEY_5 =
            "KOy+czuZWfjWYx3Mp7srueyQ7xF6/as6FWrED0BlvmhJYj0yhTOTOopAXhGNEk7cUSFxqP2FKYX8e3pHm/uNZvKcSrLXbLUhQnULhn4WmKOQIDAQAB"

        /**
         * Public key for verify purchase.
         */
        private const val PUBLIC_KEY =
            BASE64_ENCODED_PUBLIC_KEY_1 + BASE64_ENCODED_PUBLIC_KEY_2 + BASE64_ENCODED_PUBLIC_KEY_3 + BASE64_ENCODED_PUBLIC_KEY_4 + BASE64_ENCODED_PUBLIC_KEY_5

    }
}