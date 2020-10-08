package mega.privacy.android.app.sync


data class SyncPair(
    var syncId: Long,
    var name: String,
    var syncType: Int,
    var localFolderPath: String,
    var targetFodlerHanlde: Long,
    var targetFolderPath: String? = null,
    var isExcludeSubFolders: Boolean = false,
    var isDeleteEmptySubFolders: Boolean = false,
    var startTimestamp: Long = 0L,
    var lastFinishTimestamp: Long = 0L,
    var state: Int = 1,
    var subState: Int = 1,
    var extraData: String? = null,
    var outdated: Boolean = false
) {

    companion object {

        @JvmStatic
        fun create(
            syncId: Long,
            name: String,
            syncType: Int,
            localFolderPath: String,
            targetFodlerHanlde: Long
        ) = SyncPair(syncId, name, syncType, localFolderPath, targetFodlerHanlde)
    }



    override fun toString(): String {
        return "SyncPair [$syncId $syncType $targetFodlerHanlde(${targetFodlerHanlde.name()}) $localFolderPath $outdated]"
    }
}