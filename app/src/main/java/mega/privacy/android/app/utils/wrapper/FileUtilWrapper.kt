package mega.privacy.android.app.utils.wrapper

import java.io.File

interface FileUtilWrapper {
    fun getFileIfExists(folder: File? = null, fileName: String): File?{
        return File(folder, fileName).takeIf { it.exists() }
    }
}