package mega.privacy.android.app.meeting.activity

import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import mega.privacy.android.app.R

class MeetingActivity : AppCompatActivity() {
    companion object{
        const val MEETING_TYPE = "meetingType"
        const val MEETING_TYPE_JOIN = "join_meeting"
        const val MEETING_TYPE_CREATE = "create_meeting"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.meeting_activity)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        if (savedInstanceState == null) {
//            val navHostFragment =
//                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
//            val navController = navHostFragment.navController

//            supportFragmentManager.beginTransaction()
//                .replace(R.id.container, CreateMeetingFragment.newInstance())
//                .commitNow()
        }
        val navGraph: NavGraph = navHostFragment.navController.navInflater.inflate(R.navigation.meeting)
        when(intent.getStringExtra(MEETING_TYPE)){
            MEETING_TYPE_JOIN->navGraph.startDestination = R.id.joinMeetingFragment
            MEETING_TYPE_CREATE->navGraph.startDestination = R.id.createMeetingFragment
        }
        navController.graph = navGraph

    }
}