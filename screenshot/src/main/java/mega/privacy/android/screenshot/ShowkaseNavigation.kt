package mega.privacy.android.screenshot

import android.app.Activity
import com.airbnb.android.showkase.models.Showkase

fun Activity.navigateToShowkase() = startActivity(Showkase.getBrowserIntent(this))