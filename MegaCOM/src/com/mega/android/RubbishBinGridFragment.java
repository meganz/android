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

public class RubbishBinGridFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView gridView;
	MegaRubbishBinGridAdapter adapter;
	
	public static final String[] names = new String[] { "filename.ext", "filename.ext", "filename.ext", "filename.ext", "filename.ext", "filename.ext"};
	public static final Integer[] images = { R.drawable.icorb01, R.drawable.icorb02, R.drawable.icorb03, R.drawable.icorb04, R.drawable.icorb05, R.drawable.icorb06};
	
	List<ItemFileBrowser> items;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			
		View v = inflater.inflate(R.layout.fragment_rubbishbingrid, container, false);
		
		items = new ArrayList<ItemFileBrowser>();
        for (int i = 0; i < names.length; i++) {
        	ItemFileBrowser item = new ItemFileBrowser(images[i], names[i]);
            items.add(item);
        }
        
        gridView = (ListView) v.findViewById(R.id.rubbishbin_grid_view);
        gridView.setOnItemClickListener(null);
        gridView.setItemsCanFocus(false);
//        gridView.setEnabled(false);        
        
		adapter = new MegaRubbishBinGridAdapter(context, items);
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
