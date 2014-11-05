package com.mega.android;

import com.mega.android.utils.Util;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SentRequestsFragment extends Fragment{
	
	ImageView emptyImageView;
	TextView emptyTextView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate");
		
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_requests_tab, container, false);
                
        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_contacts_requests);
		emptyTextView = (TextView) v.findViewById(R.id.empty_text_contacts_requests);
		
		emptyImageView.setImageResource(R.drawable.ic_empty_folder);
		emptyTextView.setText(R.string.sent_requests_empty);
		
		emptyImageView.setVisibility(View.VISIBLE);
		emptyTextView.setVisibility(View.VISIBLE);
		
        return v;
    }

    
	private static void log(String log) {
		Util.log("SentRequestsFragment", log);
	}
}
