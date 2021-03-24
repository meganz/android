package mega.privacy.android.app.activities.textFileEditor

import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode

class TextFileEditorViewModel @ViewModelInject constructor(private val megaApi: MegaApiAndroid) :
    BaseRxViewModel() {

    companion object {
        const val CREATE_MODE = "CREATE_MODE"
        const val VIEW_MODE = "VIEW_MODE"
        const val EDIT_MODE = "EDIT_MODE"
    }

    private var fileName: String = ""
    private var node: MegaNode? = null
    private var mode = VIEW_MODE

    fun isViewMode(): Boolean = mode == VIEW_MODE

    fun setViewMode() {
        mode = VIEW_MODE
    }

    fun setEditMode() {
        mode = EDIT_MODE
    }

    fun setModeAndName(handle: Long, name: String?) {
        node = megaApi.getNodeByHandle(handle)
        mode = if (node == null || node?.isFolder == true) CREATE_MODE else VIEW_MODE
        fileName = if (name != null) name + FileUtil.TXT_EXTENSION else node?.name!!
    }

    fun getFileName(): String = fileName
}