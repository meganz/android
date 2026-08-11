package mega.privacy.android.app.fragments.settingsFragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.activities.settingsActivities.PreferencesBaseActivity
import mega.privacy.android.app.interfaces.SimpleSnackbarCallBack
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Inject

abstract class SettingsBaseFragment : PreferenceFragmentCompat() {

    @Inject
    @MegaApi
    internal lateinit var megaApi: MegaApiAndroid

    @Inject
    internal lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    internal lateinit var dbH: DatabaseHandler
    protected val prefs: MegaPreferences?
        get() = dbH.preferences
    protected var snackbarCallBack: SimpleSnackbarCallBack? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is SimpleSnackbarCallBack) {
            snackbarCallBack = context
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                (activity as? PreferencesBaseActivity)?.updateElevation(
                    recyclerView.canScrollVertically(
                        Constants.SCROLLING_UP_DIRECTION
                    )
                )
            }
        })
    }

}