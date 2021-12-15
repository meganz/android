package mega.privacy.android.app.zippreview.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.zippreview.domain.ZipFileType
import mega.privacy.android.app.zippreview.domain.ZipInfoBO
import mega.privacy.android.app.zippreview.domain.IZipFileRepo
import mega.privacy.android.app.zippreview.ui.ZipInfoUIO
import java.io.File
import java.util.zip.ZipFile

class ZipBrowserViewModel @ViewModelInject constructor(private val zipFileRepo: IZipFileRepo) :
    BaseRxViewModel() {
    companion object {
        private const val TITLE_ZIP = "ZIP "
        private const val SUFFIX_ZIP = ".zip"
    }

    private lateinit var zipFullPath: String
    private lateinit var unZipRootPath: String
    private lateinit var unknownStr: String

    private lateinit var zipFile: ZipFile

    private lateinit var currentFolderPath: String

    private var _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private var _zipInfoList = MutableLiveData<List<ZipInfoUIO>>()
    val zipInfoList: LiveData<List<ZipInfoUIO>>
        get() = _zipInfoList

    private var _showProgressDialog = MutableLiveData<Boolean>()
    val showProgressDialog: LiveData<Boolean>
        get() = _showProgressDialog

    private var _showAlert = MutableLiveData<Boolean>()
    val showAlert: LiveData<Boolean>
        get() = _showAlert

    private var _openFile = MutableLiveData<Pair<Int, ZipInfoUIO>>()
    val openFile: LiveData<Pair<Int, ZipInfoUIO>>
        get() = _openFile

    enum class TypeItemClickResult {
        TYPE_ZIP_OPEN_FOLDER, TYPE_ZIP_NOT_UNPACK, TYPE_ZIP_NOT_EXIST, TYPE_OPEN_FILE
    }

    private fun updateZipInfoList(folderPath: String) {
        viewModelScope.launch {
            _zipInfoList.value =
                zipFileRepo.updateZipInfoList(folderPath, unknownStr, zipFile).map {
                    zipInfoBOToUIO(it)
                }
            getTitle(folderPath)
        }
    }

    private fun zipInfoBOToUIO(zipInfoBO: ZipInfoBO): ZipInfoUIO {
        val imageResourceId = if (zipInfoBO.fileType == ZipFileType.FOLDER) {
            R.drawable.ic_folder_list
        } else {
            MimeTypeList.typeForName(zipInfoBO.zipFileName).iconResourceId
        }
        val displayedFileName = when (zipInfoBO.fileType) {
            ZipFileType.UNKNOWN ->
                zipInfoBO.zipFileName
            ZipFileType.FOLDER ->
                File(zipInfoBO.zipFileName).name
            else ->
                zipInfoBO.zipFileName.split("/").last()
        }
        return ZipInfoUIO(
            zipInfoBO.zipFileName,
            zipInfoBO.info,
            imageResourceId,
            displayedFileName,
            zipInfoBO.fileType
        )
    }

    fun viewModelInit(zipFullPath: String, unknownStr: String, unZipRootPath: String) {
        this.zipFullPath = zipFullPath
        this.unknownStr = unknownStr
        this.unZipRootPath = unZipRootPath
        zipFile = ZipFile(zipFullPath)
        openFolder(zipToFolder())
    }

    fun openFolder(folderPath: String) {
        currentFolderPath = folderPath
        updateZipInfoList(currentFolderPath)
    }

    fun backOnPress(): Boolean {
        if (!isZipRootFolder()) {
            val strArray = currentFolderPath.split("/")
            currentFolderPath =
                currentFolderPath.removeSuffix("${strArray[strArray.size - 2]}/")
            updateZipInfoList(currentFolderPath)
            return false
        }
        return true
    }

    fun onZipFileClicked(zipInfoUIO: ZipInfoUIO, position: Int) {
        when (getFileStatus(zipInfoUIO, unZipRootPath)) {
            TypeItemClickResult.TYPE_ZIP_NOT_UNPACK -> {
                _showProgressDialog.value = true
                //If zip folder doesn't exist, unpacked the zip file.
                unpackedZipFile(zipInfoUIO, position)
            }
            TypeItemClickResult.TYPE_OPEN_FILE ->
                _openFile.value = Pair(position, zipInfoUIO)
            TypeItemClickResult.TYPE_ZIP_OPEN_FOLDER ->
                openFolder(zipInfoUIO.zipFileName)
            TypeItemClickResult.TYPE_ZIP_NOT_EXIST -> {
                LogUtil.logError("zip entry position $position file not exists")
                _showAlert.value = true
            }
        }
    }

    private fun zipToFolder(): String {
        return if (zipFullPath.contains("/")) {
            "${zipFullPath.split("/").lastOrNull()?.removeSuffix(SUFFIX_ZIP)}/"
        } else {
            "$zipFullPath/"
        }
    }

    private fun unpackedZipFile(zipInfoUIO: ZipInfoUIO, position: Int) {
        viewModelScope.launch {
            zipFileRepo.unpackZipFile(zipFullPath, unZipRootPath)
            _showProgressDialog.value = false
            _openFile.value = Pair(position, zipInfoUIO)
        }
    }

    private fun getTitle(folderPath: String) {
        _title.value = if (isZipRootFolder()) {
            "$TITLE_ZIP${File(folderPath).name}"
        } else {
            File(folderPath).name
        }
    }

    private fun isZipRootFolder(): Boolean {
        return File(currentFolderPath).parent == null
    }

    private fun getFileStatus(zipInfoUIO: ZipInfoUIO, rootPath: String?): TypeItemClickResult {
        return if (zipInfoUIO.fileType == ZipFileType.FOLDER) {
            TypeItemClickResult.TYPE_ZIP_OPEN_FOLDER
        } else {
            val zipFolderPath = zipFullPath.split(".").first()
            val currentFile = File(rootPath + zipInfoUIO.zipFileName)
            if (File(zipFolderPath).exists()) {
                if (currentFile.exists()) {
                    TypeItemClickResult.TYPE_OPEN_FILE
                } else {
                    TypeItemClickResult.TYPE_ZIP_NOT_EXIST
                }
            } else {
                TypeItemClickResult.TYPE_ZIP_NOT_UNPACK
            }
        }
    }
}