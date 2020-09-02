package mega.privacy.android.app.fragments.offline

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.FileUtils.isFileAvailable
import mega.privacy.android.app.utils.OfflineUtils.getFolderInfo
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.OfflineUtils.getThumbnailFile
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util.getSizeString
import java.io.File

class OfflineFileInfoViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MegaNodeRepo
) : BaseRxViewModel() {

    private val _node = MutableLiveData<OfflineNode?>()
    private val _totalSize = MutableLiveData<String>()
    private val _contains = MutableLiveData<String>()
    private val _added = MutableLiveData<String>()

    val node: LiveData<OfflineNode?> = _node
    val totalSize: LiveData<String> = _totalSize
    val contains: LiveData<String> = _contains
    val added: LiveData<String> = _added

    fun loadNode(handle: String) {
        add(Single.fromCallable { repo.findOfflineNode(handle) }
            .map {
                if (it == null) {
                    return@map null
                }
                val thumbnail = if (MimeTypeList.typeForName(it.name).isImage) {
                    getOfflineFile(context, it)
                } else {
                    getThumbnailFile(context, it)
                }
                OfflineNode(it, if (isFileAvailable(thumbnail)) thumbnail else null, "", false)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer {
                _node.value = it
                if (it != null) {
                    getMetaInfo(it.node)
                }
            }, logErr("OfflineFileInfoViewModel loadNode onError")))
    }

    private fun getMetaInfo(node: MegaOffline) {
        add(Single
            .fromCallable {
                val file = getOfflineFile(context, node)
                val totalSize = getSizeString(getTotalSize(file))
                val contains = if (node.isFolder) {
                    getFolderInfo(context.resources, file)
                } else {
                    ""
                }
                val added = TimeUtils.formatDateAndTime(
                    context,
                    file.lastModified() / 1000,
                    TimeUtils.DATE_LONG_FORMAT
                )
                Triple(totalSize, contains, added)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(Consumer {
                _totalSize.value = it.first
                _added.value = it.third
                if (node.isFolder) {
                    _contains.value = it.second
                }
            }, logErr("OfflineFileInfoViewModel getMetaInfo onError"))
        )
    }

    private fun getTotalSize(file: File): Long {
        if (file.isFile) {
            return file.length()
        }
        val files = file.listFiles() ?: return 0L

        var totalSize = 0L
        files.forEach {
            totalSize += if (it.isFile) {
                it.length()
            } else {
                getTotalSize(it)
            }
        }
        return totalSize
    }
}
