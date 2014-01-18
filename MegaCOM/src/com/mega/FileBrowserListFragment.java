package com.mega;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserListFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	
	public static final String[] names = new String[] { "salamanca01.png", "salamanca02.png", "salamanca03.png", "salamanca04.png", "salamanca05.png", "salamanca06.png", "salamanca07.png", "salamanca08.png", "salamanca09.png", "salamanca10.png"};
	public static final Integer[] images = { R.drawable.sal01, R.drawable.sal02, R.drawable.sal03, R.drawable.sal04, R.drawable.sal05, R.drawable.sal06, R.drawable.sal07, R.drawable.sal08, R.drawable.sal09, R.drawable.sal10};
	
	List<ItemFileListBrowser> rowItems;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_filebrowserlist, container, false);
		
		rowItems = new ArrayList<ItemFileListBrowser>();
        for (int i = 0; i < names.length; i++) {
        	ItemFileListBrowser item = new ItemFileListBrowser(images[i], names[i]);
            rowItems.add(item);
        }
        
        listView = (ListView) v.findViewById(R.id.file_list_view_browser);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		MegaBrowserAdapter adapter = new MegaBrowserAdapter(context, rowItems);
		listView.setAdapter(adapter);
		
//		bVer = (Button) v.findViewById(R.id.buttonVer);
//		bOcultar = (Button) v.findViewById(R.id.buttonOcultar);
//		bVer.setOnClickListener(this);
//		bOcultar.setOnClickListener(this);
		
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((ActionBarActivity)activity).getSupportActionBar();
    }
	
	@Override
	public void onClick(View v) {

		switch(v.getId()){
//			case R.id.buttonVer:
//				onClickVer(v);
//				break;
//			case R.id.buttonOcultar:
//				onClickOcultar(v);
//				break;
		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Toast toast = Toast.makeText(context,
                "Item " + (position + 1) + ": " + rowItems.get(position),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }
	
	
//	public void onClickOcultar(View v){
//		aB.hide();
//	}
//	
//	public void onClickVer(View v){
//		if (!aB.isShowing())
//			aB.show();
//
//	}	

}
