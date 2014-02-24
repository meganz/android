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

public class ContactsGridFragment extends Fragment implements OnClickListener, OnItemClickListener{

	Context context;
	ActionBar aB;
	ListView gridView;
	MegaContactsGridAdapter adapter;
	
//	public static final String[] names = new String[] { "salamanca01.png", "salamanca02.png", "salamanca03.png", "salamanca04.png", "salamanca05.png", "salamanca06.png", "salamanca07.png", "salamanca08.png", "salamanca09.png", "salamanca10.png"};
//	public static final Integer[] images = { R.drawable.sal01, R.drawable.sal02, R.drawable.sal03, R.drawable.sal04, R.drawable.sal05, R.drawable.sal06, R.drawable.sal07, R.drawable.sal08, R.drawable.sal09, R.drawable.sal10};
	public static final String[] names = new String[] { "Jesús Aragón 1", "Jesús Aragón 2", "Jesús Aragón 3", "Jesús Aragón 4", "Jesús Aragón 1", "Jesús Aragón 2", "Jesús Aragón 3", "Jesús Aragón 4", "Jesús Aragón 1", "Jesús Aragón 2", "Jesús Aragón 3", "Jesús Aragón 4"};
	public static final Integer[] images = { R.drawable.jesus, R.drawable.jesus, R.drawable.jesus, R.drawable.jesus,  R.drawable.jesus, R.drawable.jesus, R.drawable.jesus, R.drawable.jesus,  R.drawable.jesus, R.drawable.jesus, R.drawable.jesus, R.drawable.jesus};
	
	List<ItemContact> items;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		if (aB == null){
			aB = ((ActionBarActivity)context).getSupportActionBar();
		}
		aB.setTitle(getString(R.string.section_contacts));

		View v = inflater.inflate(R.layout.fragment_contactsgrid, container, false);
		
		items = new ArrayList<ItemContact>();
        for (int i = 0; i < names.length; i++) {
        	ItemContact item = new ItemContact(images[i], names[i]);
            items.add(item);
        }
        
        gridView = (ListView) v.findViewById(R.id.contact_grid_view_browser);
        gridView.setOnItemClickListener(null);
        gridView.setItemsCanFocus(false);
//        gridView.setEnabled(false);        
        
		adapter = new MegaContactsGridAdapter(context, items);
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
