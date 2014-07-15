package com.mega.android;

import com.mega.sdk.MegaApiJava;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

public class SortByDialogActivity extends Activity implements OnClickListener {

	public static String ACTION_SORT_BY = "ACTION_SORT_BY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_sort_by_dialog);	
		ImageButton defaultAsc = (ImageButton) findViewById(R.id.sort_by_default_asc);
		defaultAsc.setOnClickListener(this);
		ImageButton defaultDesc = (ImageButton) findViewById(R.id.sort_by_default_desc);
		defaultDesc.setOnClickListener(this);
		ImageButton azAsc = (ImageButton) findViewById(R.id.sort_by_az_asc);
		azAsc.setOnClickListener(this);
		ImageButton azDesc = (ImageButton) findViewById(R.id.sort_by_az_desc);
		azDesc.setOnClickListener(this);
		ImageButton sizeAsc = (ImageButton) findViewById(R.id.sort_by_size_asc);
		sizeAsc.setOnClickListener(this);
		ImageButton sizeDesc = (ImageButton) findViewById(R.id.sort_by_size_desc);
		sizeDesc.setOnClickListener(this);
		ImageButton createdAsc = (ImageButton) findViewById(R.id.sort_by_created_asc);
		createdAsc.setOnClickListener(this);
		ImageButton createdDesc = (ImageButton) findViewById(R.id.sort_by_created_desc);
		createdDesc.setOnClickListener(this);
		ImageButton modifiedAsc = (ImageButton) findViewById(R.id.sort_by_modified_asc);
		modifiedAsc.setOnClickListener(this);
		ImageButton modifiedDesc = (ImageButton) findViewById(R.id.sort_by_modified_desc);
		modifiedDesc.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		int orderGetChildren = 0;
		switch(v.getId()){
			case R.id.sort_by_default_asc:{
				orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
				break;
			}
			case R.id.sort_by_default_desc:{
				orderGetChildren = MegaApiJava.ORDER_DEFAULT_DESC;
				break;
			}
			
			case R.id.sort_by_az_asc:{
				orderGetChildren = MegaApiJava.ORDER_ALPHABETICAL_ASC;
				break;
			}
			case R.id.sort_by_az_desc:{
				orderGetChildren = MegaApiJava.ORDER_ALPHABETICAL_DESC;
				break;
			}
			
			case R.id.sort_by_size_asc:{
				orderGetChildren = MegaApiJava.ORDER_SIZE_ASC;
				break;
			}
			case R.id.sort_by_size_desc:{
				orderGetChildren = MegaApiJava.ORDER_SIZE_DESC;
				break;
			}
			
			case R.id.sort_by_created_asc:{
				orderGetChildren = MegaApiJava.ORDER_CREATION_ASC;
				break;
			}
			case R.id.sort_by_created_desc:{
				orderGetChildren = MegaApiJava.ORDER_CREATION_DESC;
				break;
			}
			
			case R.id.sort_by_modified_asc:{
				orderGetChildren = MegaApiJava.ORDER_MODIFICATION_ASC;
				break;
			}
			case R.id.sort_by_modified_desc:{
				orderGetChildren = MegaApiJava.ORDER_MODIFICATION_DESC;
				break;
			}
			default:{
				orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;
				break;
			}
		}
		Intent intent = new Intent();
		intent.putExtra("ORDER_GET_CHILDREN", orderGetChildren);
		setResult(RESULT_OK, intent);
		finish();
	}
	
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
