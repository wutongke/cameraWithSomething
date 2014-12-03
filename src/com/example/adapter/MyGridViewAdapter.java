package com.example.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class MyGridViewAdapter extends BaseAdapter{
	
	private ArrayList<String> picturePaths ;
	private Context mContext;
	
	public MyGridViewAdapter(Context context,ArrayList<String> picturePaths) {
		// TODO Auto-generated constructor stub
		mContext = context;
		this.picturePaths = picturePaths;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return picturePaths.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return picturePaths.get(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		if(convertView==null){
			
		}
		
		
		return null;
	}
	
	class ViewHolder{
		ImageView myImage;
	}
	
	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

}
