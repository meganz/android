package mega.privacy.android.app.interfaces

interface PermissionRequester {
    fun askPermissions(permissions: Array<String>, requestCode: Int)

    companion object {
        val IDLE = object : PermissionRequester {
            override fun askPermissions(permissions: Array<String>, requestCode: Int) {}
        }
    }
}
