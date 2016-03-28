package mega.privacy.android.app.providers;

import mega.privacy.android.app.PinUtil;

public class PinFileProviderActivity extends FileProviderActivity {
	
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
