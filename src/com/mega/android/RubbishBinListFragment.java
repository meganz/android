package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class RubbishBinListFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView listView;
	MegaRubbishBinListAdapter adapter;
	
	public static final String[] names = new String[] { "filename.ext", "filename.ext", "filename.ext", "filename.ext", "filename.ext", "filename.ext"};
	public static final Integer[] images = { R.drawable.icorb01, R.drawable.icorb02, R.drawable.icorb03, R.drawable.icorb04, R.drawable.icorb05, R.drawable.icorb06};
	
	List<ItemFileBrowser> rowItems;
	ArrayList<Integer> imageIds;
	ArrayList<String> namesArray;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_rubbishbinlist, container, false);
		
		namesArray = new ArrayList<String>();
		imageIds = new ArrayList<Integer>();
		rowItems = new ArrayList<ItemFileBrowser>();
        for (int i = 0; i < names.length; i++) {
        	ItemFileBrowser item = new ItemFileBrowser(images[i], names[i]);
            rowItems.add(item);
            imageIds.add(item.getImageId());
            namesArray.add(item.getName());
        }
        
        listView = (ListView) v.findViewById(R.id.rubbishbin_list_view);
		listView.setOnItemClickListener(this);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setItemsCanFocus(false);
		adapter = new MegaRubbishBinListAdapter(context, rowItems);
		adapter.setPositionClicked(-1);
		listView.setAdapter(adapter);
		
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
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
		
		Intent i = new Intent(context, FullScreenImageViewer.class);
		i.putExtra("position", position);
		i.putExtra("imageIds", imageIds);
		i.putExtra("names", namesArray);
		startActivity(i);
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
