/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mega.privacy.android.app.utils.billing;

import android.text.TextUtils;
import android.util.Base64;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import static mega.privacy.android.app.utils.LogUtil.*;

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
public class Security {
    private static final String TAG = "IABUtil/Security";

    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    private static final String BASE64_ENCODED_PUBLIC_KEY_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0bZjbgdGRd6/hw5/J2FGTkdG";
    private static final String BASE64_ENCODED_PUBLIC_KEY_2 = "tDTMdR78hXKmrxCyZUEvQlE/DJUR9a/2ZWOSOoaFfi9XTBSzxrJCIa+gjj5wkyIwIrzEi";
    private static final String BASE64_ENCODED_PUBLIC_KEY_3 = "55k9FIh3vDXXTHJn4oM9JwFwbcZf1zmVLyes5ld7+G15SZ7QmCchqfY4N/a/qVcGFsfwqm";
    private static final String BASE64_ENCODED_PUBLIC_KEY_4 = "RU3VzOUwAYHb4mV/frPctPIRlJbzwCXpe3/mrcsAP+k6ECcd19uIUCPibXhsTkNbAk8CRkZ";
    private static final String BASE64_ENCODED_PUBLIC_KEY_5 = "KOy+czuZWfjWYx3Mp7srueyQ7xF6/as6FWrED0BlvmhJYj0yhTOTOopAXhGNEk7cUSFxqP2FKYX8e3pHm/uNZvKcSrLXbLUhQnULhn4WmKOQIDAQAB";
    private static final String BASE_64_ENCODED_PUBLIC_KEY = BASE64_ENCODED_PUBLIC_KEY_1 + BASE64_ENCODED_PUBLIC_KEY_2 + BASE64_ENCODED_PUBLIC_KEY_3 + BASE64_ENCODED_PUBLIC_KEY_4 + BASE64_ENCODED_PUBLIC_KEY_5;

    /**
     * Verifies that the data was signed with the given signature, and returns the verified
     * purchase.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    public static boolean verifyPurchase(String signedData, String signature) throws IOException {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(BASE_64_ENCODED_PUBLIC_KEY)
                || TextUtils.isEmpty(signature)) {
            logWarning("Purchase verification failed: missing data.");
            return false;
        }

        PublicKey key = generatePublicKey(BASE_64_ENCODED_PUBLIC_KEY);
        return verify(key, signedData, signature);
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IOException if encoding algorithm is not supported or key specification
     * is invalid
     */
    public static PublicKey generatePublicKey(String encodedPublicKey) throws IOException {
        try {
            byte[] decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            return keyFactory.generatePublic(new X509EncodedKeySpec(decodedKey));
        } catch (NoSuchAlgorithmException e) {
            // "RSA" is guaranteed to be available.
            logError("RSA is unavailable.", e);
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            String msg = "Invalid key specification: " + e;
            logWarning(msg, e);
            throw new IOException(msg);
        } catch (IllegalArgumentException e) {
            logWarning(e.getMessage(), e);
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey public key associated with the developer account
     * @param signedData signed data from server
     * @param signature server signature
     * @return true if the data and signature match
     */
    public static boolean verify(PublicKey publicKey, String signedData, String signature) {
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            logWarning("Base64 decoding failed.", e);
            return false;
        }
        try {
            Signature signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM);
            signatureAlgorithm.initVerify(publicKey);
            signatureAlgorithm.update(signedData.getBytes());
            if (!signatureAlgorithm.verify(signatureBytes)) {
                logWarning("Signature verification failed.");
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException e) {
            // "RSA" is guaranteed to be available.
            logError("RSA is unavailable.", e);
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            logWarning("Invalid key specification.", e);
        } catch (SignatureException e) {
            logWarning("Signature exception.", e);
        }
        return false;
    }
}
