package mega.privacy.android.app;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class LauncherFileExplorerActivity extends PinActivity{
	
	ManagerActivity mActivity;
	ManagerActivityLollipop mActivityLol;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		log("onCreate");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {	
			log("Lollipop Version");
			Intent intent = getIntent();
			Intent fileExplorerIntent = new Intent(this, FileExplorerActivityLollipop.class);
			fileExplorerIntent.setAction(ManagerActivity.ACTION_FILE_EXPLORER_UPLOAD);
			if (intent != null){
				if(intent.getExtras() != null){
					fileExplorerIntent.putExtras(intent.getExtras());
				}
					
				if(intent.getData() != null){
					fileExplorerIntent.setData(intent.getData());
				}
				startActivity(fileExplorerIntent);
				finish();
			}
		} else {
			log("Older Version");
			Intent intent = getIntent();
			Intent fileExplorerIntent = new Intent(this, FileExplorerActivity.class);
			fileExplorerIntent.setAction(ManagerActivity.ACTION_FILE_EXPLORER_UPLOAD);
			if (intent != null){
				if(intent.getExtras() != null){
					fileExplorerIntent.putExtras(intent.getExtras());
				}
					
				if(intent.getData() != null){
					fileExplorerIntent.setData(intent.getData());
				}
				startActivity(fileExplorerIntent);
				finish();
			}
		}
		super.onCreate(savedInstanceState);
	}
	
	public static void log(String message) {
		Util.log("LauncherFileExplorerActivity", message);
	}
}
