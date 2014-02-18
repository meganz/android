package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class FileBrowserGridFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView gridView;
	MegaBrowserGridAdapter adapter;
	
//	public static final String[] names = new String[] { "salamanca01.png", "salamanca02.png", "salamanca03.png", "salamanca04.png", "salamanca05.png", "salamanca06.png", "salamanca07.png", "salamanca08.png", "salamanca09.png", "salamanca10.png"};
//	public static final Integer[] images = { R.drawable.sal01, R.drawable.sal02, R.drawable.sal03, R.drawable.sal04, R.drawable.sal05, R.drawable.sal06, R.drawable.sal07, R.drawable.sal08, R.drawable.sal09, R.drawable.sal10};
	public static final String[] names = new String[] { "salamanca01.png", "salamanca02.png", "salamanca03.png", "salamanca04.png", "salamanca05.png", "salamanca06.png", "salamanca07.png", "salamanca08.png", "salamanca09.png", "salamanca10.png", "salamanca11.png", "salamanca12.png", "salamanca13.png", "salamanca14.png", "salamanca15.png", "salamanca16.png", "salamanca17.png", "salamanca18.png", "salamanca19.png"};
	public static final Integer[] images = { R.drawable.sal01, R.drawable.sal10, R.drawable.sal03, R.drawable.sal10, R.drawable.sal05, R.drawable.sal10, R.drawable.sal07, R.drawable.sal10, R.drawable.sal09, R.drawable.sal10, R.drawable.sal09, R.drawable.sal10, R.drawable.sal09, R.drawable.sal10, R.drawable.sal09, R.drawable.sal10, R.drawable.sal09, R.drawable.sal10, R.drawable.sal09};
	
	List<ItemFileBrowser> items;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			
		aB.setTitle(getString(R.string.section_cloud_drive));

		View v = inflater.inflate(R.layout.fragment_filebrowsergrid, container, false);
		
		items = new ArrayList<ItemFileBrowser>();
        for (int i = 0; i < names.length; i++) {
        	ItemFileBrowser item = new ItemFileBrowser(images[i], names[i]);
            items.add(item);
        }
        
        gridView = (ListView) v.findViewById(R.id.file_grid_view_browser);
        gridView.setOnItemClickListener(null);
        gridView.setItemsCanFocus(false);
//        gridView.setEnabled(false);        
        
		adapter = new MegaBrowserGridAdapter(context, items);
		adapter.setPositionClicked(-1);
		gridView.setAdapter(adapter);
		
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

		}
	}
	
	@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
//		Intent i = new Intent(context, FilePropertiesActivity.class);
//		i.putExtra("imageId", items.get(position).getImageId());
//		i.putExtra("name", items.get(position).getName());
//		startActivity(i);
    }
	
	public int onBackPressed(){
		
		if (adapter.getPositionClicked() != -1){
			adapter.setPositionClicked(-1);
			adapter.notifyDataSetChanged();
			return 1;
		}
		else{
			return 0;
		}
	}
	
	

}
