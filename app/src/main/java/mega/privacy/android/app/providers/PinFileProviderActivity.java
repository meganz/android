package mega.privacy.android.app.providers;

import androidx.appcompat.app.AppCompatActivity;
import mega.privacy.android.app.PinUtil;

public class PinFileProviderActivity extends AppCompatActivity {
	
	@Override
	protected void onPause() {
		PinUtil.pause(this);
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		PinUtil.resume(this);
	}
}
