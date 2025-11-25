package mega.privacy.android.navigation.contract.queue

sealed class NavPriority : Comparable<NavPriority> {
    object Default : NavPriority()
    data class Priority(val level: Int) : NavPriority()

    override fun compareTo(other: NavPriority): Int {
        val thisLevel = if (this is Priority) this.level else 0
        val otherLevel = if (other is Priority) other.level else 0
        return thisLevel.compareTo(otherLevel)
    }
}
