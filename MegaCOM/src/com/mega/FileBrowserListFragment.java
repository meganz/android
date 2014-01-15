package com.mega;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class FileBrowserListFragment extends Fragment implements OnClickListener{

	private ActionBar aB;
	private Button bVer;
	private Button bOcultar;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		bVer = (Button) v.findViewById(R.id.buttonVer);
		bOcultar = (Button) v.findViewById(R.id.buttonOcultar);
		bVer.setOnClickListener(this);
		bOcultar.setOnClickListener(this);
		
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
			case R.id.buttonVer:
				onClickVer(v);
				break;
			case R.id.buttonOcultar:
				onClickOcultar(v);
				break;
		}
	}
	
	
	public void onClickOcultar(View v){
		aB.hide();
	}
	
	public void onClickVer(View v){
		if (!aB.isShowing())
			aB.show();

	}	

}
