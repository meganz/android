package mega.privacy.android.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Hilt test activity
 *
 * An empty activity that implements [AndroidEntryPoint] used to host fragments for ui tests
 */
@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hilt_test_activity)
        setSupportActionBar(findViewById(R.id.toolbar))
    }
}
