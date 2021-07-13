package mega.privacy.android.app.utils

import android.util.Base64
import nz.mega.sdk.MegaChatApiJava

object MegaChatApiJavaUtils {

    /**
     * Since getUserAliasFromCache() method temporarily returns the value encoded in Base64, this
     * method returns the alias decoded accordingly.
     */
    @JvmStatic
    fun MegaChatApiJava.getUserAliasFromCacheDecoded(userHandle: Long): String? =
        getUserAliasFromCache(userHandle)?.let { aliasBase64 ->
            Base64.decode(aliasBase64, Base64.DEFAULT).toString(Charsets.UTF_8)
        }
}
