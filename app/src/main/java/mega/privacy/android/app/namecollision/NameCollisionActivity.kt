package mega.privacy.android.app.namecollision

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import mega.privacy.android.app.databinding.ActivityNameCollisionBinding
import mega.privacy.android.app.domain.entity.NameCollision
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_COLLISION_RESULTS

/**
 * Activity for showing name collisions and resolving them as per user's choices.
 */
class NameCollisionActivity : AppCompatActivity() {

    companion object {

        @JvmStatic
        fun getIntentForList(
            context: Context,
            collisions: ArrayList<NameCollision>
        ): Intent =
            Intent(context, NameCollisionActivity::class.java).apply {
                putExtra(INTENT_EXTRA_COLLISION_RESULTS, collisions)
            }

        @JvmStatic
        fun getIntentSingleItem(
            context: Context,
            collision: NameCollision
        ): Intent =
            Intent(context, NameCollisionActivity::class.java).apply {
                putExtra(INTENT_EXTRA_COLLISION_RESULTS, collision)
            }
    }

    private val viewModel: NameCollisionViewModel by viewModels()

    private lateinit var binding: ActivityNameCollisionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameCollisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupData(savedInstanceState)
        setupView()
        setupObservers()
    }

    private fun setupData(savedInstanceState: Bundle?) {

    }

    private fun setupView() {

    }

    private fun setupObservers() {

    }
}