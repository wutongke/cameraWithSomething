package com.example.camera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.GridView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.examaple.camera.R;

public class PictureShowActivity extends Activity {

	@InjectView(R.id.ib_camera_grid)
	GridView myGridView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture_show);
		ButterKnife.inject(this);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.picture_show, menu);
		return true;
	}

}
