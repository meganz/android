package mega.privacy.android.app.zippreview.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.zippreview.domain.FileType
import mega.privacy.android.app.zippreview.domain.ZipInfoBO
import mega.privacy.android.app.zippreview.domain.IZipFileRepo
import mega.privacy.android.app.zippreview.ui.ZipInfoUIO
import java.io.File
import java.util.zip.ZipFile

/**
 * ViewModel regarding to zip preview
 * @param zipFileRepo ZipFileRepo
 */
class ZipBrowserViewModel @ViewModelInject constructor(private val zipFileRepo: IZipFileRepo) :
    ViewModel() {
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

    /**
     * The type of clicked item
     */
    enum class StatusItemClicked {
        OPEN_FOLDER, ZIP_NOT_UNPACK, ITEM_NOT_EXIST, OPEN_FILE
    }

    /**
     * Update zip info list
     * @param folderPath the path of folder, default value is ""
     */
    private fun updateZipInfoList(folderPath: String = "") {
        viewModelScope.launch {
            _zipInfoList.value =
                zipFileRepo.updateZipInfoList(unknownStr, zipFile, folderPath).map {
                    zipInfoBOToUIO(it)
                }
            getTitle(folderPath)
        }
    }

    /**
     * Convert ZipInfoBO to ZipInfoUIO
     * @param zipInfoBO ZipInfoBO
     * @return ZipInfoUIO
     */
    private fun zipInfoBOToUIO(zipInfoBO: ZipInfoBO): ZipInfoUIO {
        val imageResourceId = if (zipInfoBO.fileType == FileType.FOLDER) {
            R.drawable.ic_folder_list
        } else {
            MimeTypeList.typeForName(zipInfoBO.zipFileName).iconResourceId
        }
        val displayedFileName = when (zipInfoBO.fileType) {
            FileType.UNKNOWN -> zipInfoBO.zipFileName
            FileType.FOLDER -> File(zipInfoBO.zipFileName).name
            else -> zipInfoBO.zipFileName.split("/").last()
        }
        return ZipInfoUIO(
            zipInfoBO.zipFileName,
            zipInfoBO.info,
            imageResourceId,
            displayedFileName,
            zipInfoBO.fileType
        )
    }

    /**
     * Init ViewModel and open current zip file.
     * @param zipFullPath zip file full path
     * @param unknownStr unknown string
     * @param unZipRootPath unzip root path
     */
    fun viewModelInit(zipFullPath: String, unknownStr: String, unZipRootPath: String) {
        this.zipFullPath = zipFullPath
        this.unknownStr = unknownStr
        this.unZipRootPath = unZipRootPath
        zipFile = ZipFile(zipFullPath)
        openFolder()
    }

    /**
     * Open inside folder of zip file
     * @param folderPath the path of folder opened, default value is ""
     */
    fun openFolder(folderPath: String = "") {
        currentFolderPath = folderPath
        updateZipInfoList(currentFolderPath)
    }

    /**
     * Validation current folder if the root folder when the back button clicked.
     * @return if true, use super.onBackPress(). If false, return parent directory
     */
    fun backOnPress(): Boolean {
        if (!isZipRootDirectory()) {
            //The last item of split with "/" is "" if the string is end with "/".
            // Remove the second last item of split with "/" to get previous directory path
            val folderName = currentFolderPath.split("/").takeLast(2).first()
            currentFolderPath = currentFolderPath.removeSuffix("$folderName/")
            updateZipInfoList(currentFolderPath)
            return false
        }
        return true
    }

    /**
     * Behaviours according the status of clicked file
     * @param zipInfoUIO ZipInfoUIO
     * @param position the position of clicked file
     */
    fun onZipFileClicked(zipInfoUIO: ZipInfoUIO, position: Int) {
        when (getItemClickedStatus(zipInfoUIO, unZipRootPath)) {
            StatusItemClicked.ZIP_NOT_UNPACK -> {
                _showProgressDialog.value = true
                //If zip folder doesn't exist, unpacked the zip file.
                unpackedZipFile(zipInfoUIO, position)
            }
            StatusItemClicked.OPEN_FILE ->
                _openFile.value = Pair(position, zipInfoUIO)
            StatusItemClicked.OPEN_FOLDER ->
                openFolder(zipInfoUIO.zipFileName)
            StatusItemClicked.ITEM_NOT_EXIST -> {
                LogUtil.logError("zip entry position $position file not exists")
                _showAlert.value = true
            }
        }
    }

    /**
     * Unpack zip file and open the current clicked file.
     * @param zipInfoUIO ZipInfoUIO of clicked file
     * @param position position of clicked file
     */
    private fun unpackedZipFile(zipInfoUIO: ZipInfoUIO, position: Int) {
        viewModelScope.launch {
            zipFileRepo.unzipFile(zipFullPath, unZipRootPath)
            _showProgressDialog.value = false
            _openFile.value = Pair(position, zipInfoUIO)
        }
    }

    /**
     * Get title of actionbar
     * @param folderPath current folder path
     */
    private fun getTitle(folderPath: String) {
        //If the folder is zip root directory, title is zip filename. If not, title is folder name
        _title.value = if (isZipRootDirectory()) {
            "${TITLE_ZIP}${zipFullPath.split("/").lastOrNull()?.removeSuffix(SUFFIX_ZIP)}"
        } else {
            File(folderPath).name
        }
    }

    /**
     * Validation that current folder if the zip root directory
     * @return true is zip root directory
     */
    private fun isZipRootDirectory(): Boolean {
        return File(currentFolderPath).parent == null
    }

    /**
     * Get file status of clicked file
     * @param zipInfoUIO ZipInfoUIO of clicked file
     * @param rootPath unzip root path
     * @return TypeItemClickResult
     */
    private fun getItemClickedStatus(zipInfoUIO: ZipInfoUIO, rootPath: String): StatusItemClicked {
        return if (zipInfoUIO.fileType == FileType.FOLDER) {
            StatusItemClicked.OPEN_FOLDER
        } else {
            val zipFolderPath = zipFullPath.split(".").first()
            val currentFile = File(rootPath + zipInfoUIO.zipFileName)
            if (File(zipFolderPath).exists()) {
                if (currentFile.exists()) {
                    StatusItemClicked.OPEN_FILE
                } else {
                    StatusItemClicked.ITEM_NOT_EXIST
                }
            } else {
                StatusItemClicked.ZIP_NOT_UNPACK
            }
        }
    }
}