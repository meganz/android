package mega.privacy.android.app.fragments.offline

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.FileUtil.getFileFolderInfo
import mega.privacy.android.app.utils.FileUtil.getTotalSize
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.app.utils.wrapper.GetOfflineThumbnailFileWrapper
import javax.inject.Inject

@HiltViewModel
class OfflineFileInfoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: MegaNodeRepo,
    private val offlineThumbnailFileWrapper: GetOfflineThumbnailFileWrapper,
) : BaseRxViewModel() {

    private val _node = MutableLiveData<OfflineNode?>()
    private val _totalSize = MutableLiveData<String?>()
    private val _contains = MutableLiveData<String?>()
    private val _added = MutableLiveData<String?>()

    val node: LiveData<OfflineNode?> = _node
    val totalSize: LiveData<String?> = _totalSize
    val contains: LiveData<String?> = _contains
    val added: LiveData<String?> = _added

    fun loadNode(handle: String) {
        add(Maybe.fromCallable<MegaOffline> {
            repo.findOfflineNode(handle)
                ?: throw NullPointerException()
        }
            .map {
                val thumbnail = if (MimeTypeList.typeForName(it.name).isImage) {
                    getOfflineFile(context, it)
                } else {
                    offlineThumbnailFileWrapper.getThumbnailFile(context, it)
                }
                OfflineNode(it, if (isFileAvailable(thumbnail)) thumbnail else null, "", false)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                _node.value = it
                getMetaInfo(it.node)
            }, logErr("OfflineFileInfoViewModel loadNode")))
    }

    private fun getMetaInfo(node: MegaOffline) {
        add(Single
            .fromCallable {
                val file = getOfflineFile(context, node)
                val totalSize = getSizeString(getTotalSize(file))
                val contains = if (node.isFolder) {
                    getFileFolderInfo(file)
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
            .subscribe({
                _totalSize.value = it.first
                _added.value = it.third
                if (node.isFolder) {
                    _contains.value = it.second
                }
            }, logErr("OfflineFileInfoViewModel getMetaInfo"))
        )
    }
}
