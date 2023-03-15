package mega.privacy.android.app.presentation.twofactorauthentication.extensions


internal fun String.toSeedArray(): ArrayList<String> {
    val LENGTH_SEED = 13
    var index = 0
    val seedArray = ArrayList<String>()
    for (i in 0 until LENGTH_SEED) {
        seedArray.add(this.substring(index, index + 4))
        index += 4
    }
    return seedArray

}