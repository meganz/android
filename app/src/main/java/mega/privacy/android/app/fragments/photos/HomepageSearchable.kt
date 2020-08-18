package mega.privacy.android.app.fragments.photos

interface HomepageSearchable {
    fun shouldShowSearchMenu(): Boolean {
        return true
    }
    fun searchReady() {}
    fun searchDone() {}
    fun exitSearch() {}
    fun searchQuery(query: String) {}
}