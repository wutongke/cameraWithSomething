package com.values.camera.resocamera;

import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.camera.R;

import com.nostra13.universalimageloader.cache.memory.impl.LRULimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.values.camera.widget.CameraView;
import com.values.camera.widget.FocusView;

public class CameraActivity extends Activity implements
		CameraView.OnCameraSelectListener, View.OnClickListener {

	private CameraView cameraView;
	private Context mContext;
	public static DisplayMetrics metric = new DisplayMetrics();

	DisplayImageOptions options = new DisplayImageOptions.Builder()
			.bitmapConfig(Bitmap.Config.RGB_565)
			.imageScaleType(ImageScaleType.EXACTLY).considerExifParams(true)
			.cacheInMemory(false).cacheOnDisk(false)
			.displayer(new FadeInBitmapDisplayer(0)).build();

	private RelativeLayout rlTop;
	private RelativeLayout rlBotton;

	private ImageButton ib_camera_change;
	private ImageButton ib_camera_flash;
	private ImageButton ib_camera_grid;

	private ImageButton ibTakePicture;
	private TextView locateView;

	private ImageView imgGrid;
	private Handler uiHandler;
	private static final int SAVE_SUCCEED = 1;

	private ConnectivityManager connectivityManager;
	private NetworkInfo info;
	private NotificationManager notificationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		initImaLoader();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		/**
		 * 监听网络
		 */
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mReceiver, mFilter);

		uiHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				switch (msg.what) {
				case SAVE_SUCCEED:
					Toast.makeText(mContext, "照片保存成功", Toast.LENGTH_SHORT)
							.show();
					mContext.sendBroadcast(new Intent(
							Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
									+ Environment.getExternalStorageDirectory()
									+ "MyCamera")));
					break;
				default:
					break;
				}
			}
		};
		try {
			cameraView = new CameraView(this);
			cameraView.setOnCameraSelectListener(this);
			cameraView.setFocusView((FocusView) findViewById(R.id.sf_focus));
			cameraView.setCameraView(
					(SurfaceView) findViewById(R.id.sf_camera),
					getScreenWidth(), CameraView.MODE4T3);
		} catch (Exception e) {
			e.printStackTrace();
		}
		initViews();
		// showDCIM();
		// 获取位置信息
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// 返回所有已知的位置提供者的名称列表，包括未获准访问或调用活动目前已停用的。
		List<String> lp = lm.getAllProviders();
		for (String item : lp) {
			Log.i("8023", "可用位置服务：" + item);
		}

		Criteria criteria = new Criteria();
		criteria.setCostAllowed(false);
		// 设置位置服务免费
		criteria.setAccuracy(Criteria.ACCURACY_COARSE); 
		// getBestProvider 只有允许访问调用活动的位置供应商将被返回
		String providerName = lm.getBestProvider(criteria, true);

		if (providerName != null) {
			Location location = lm.getLastKnownLocation(providerName);
			Log.i("8023", "-------" + location);
			// 获取维度信息
			
		} else {
			Toast.makeText(this, "1.请检查网络连接 \n2.请打开我的位置", Toast.LENGTH_SHORT)
					.show();
		}

	}

	private void initViews() {
		rlTop = (RelativeLayout) findViewById(R.id.rl_top);
		rlBotton = (RelativeLayout) findViewById(R.id.rl_bottom);
		ib_camera_change = (ImageButton) findViewById(R.id.ib_camera_change);
		ib_camera_flash = (ImageButton) findViewById(R.id.ib_camera_flash);
		ib_camera_grid = (ImageButton) findViewById(R.id.ib_camera_grid);
		ibTakePicture = (ImageButton) findViewById(R.id.ib_camera_take_picture);
		locateView = (TextView) findViewById(R.id.locate);
		imgGrid = (ImageView) findViewById(R.id.img_grid);
		// rlCamera.setLayoutParams(new LayoutParams(getResources()
		// .getDisplayMetrics()., getResources()
		// .getDisplayMetrics().heightPixels
		// - rlTop.getHeight()
		// - rlBotton.getHeight()));
	}

	private void initImaLoader() {
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext()).diskCacheFileCount(50)
				.threadPoolSize(Thread.NORM_PRIORITY - 2)
				.denyCacheImageMultipleSizesInMemory().memoryCacheSize(30)
				.memoryCache(new LRULimitedMemoryCache(30)).build();
		ImageLoader.getInstance().init(config);
	}

	// /**
	// * get first picture DCIM
	// */
	// private void showDCIM() {
	// String columns[] = new String[] { MediaStore.Images.Media.DATA,
	// MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE,
	// MediaStore.Images.Media.DISPLAY_NAME };
	// Cursor cursor = this.getContentResolver().query(
	// MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
	// null, null);
	// boolean isOK = false;
	// if (cursor != null) {
	// cursor.moveToLast();
	// String path = "";
	// try {
	// while (!isOK) {
	// int photoIndex = cursor
	// .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	// path = cursor.getString(photoIndex);
	// isOK = !(path.indexOf("DCIM/Camera") == -1); // Is thie
	// // photo
	// // from DCIM
	// // folder ?
	// cursor.moveToPrevious(); // Add this so we don't get an
	// // infinite loop if the first
	// // image from
	// // the cursor is not from DCIM
	// }
	// } catch (Exception e) {
	// e.printStackTrace();
	// } finally {
	// cursor.close();
	// }
	// ImageLoader.getInstance().displayImage("file://" + path,
	// ibCameraPhotos, options);
	// }
	// }

	@Override
	protected void onStart() {
		super.onStart();
		ib_camera_change.setOnClickListener(this);
		ib_camera_flash.setOnClickListener(this);
		ib_camera_grid.setOnClickListener(this);
		ibTakePicture.setOnClickListener(this);
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			cameraView.onResume();
			cameraView.setTopDistance(rlTop.getHeight());
			cameraView.setBottonDistance(rlBotton.getHeight());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (cameraView != null)
			cameraView.onPause();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ib_camera_change:
			cameraView.changeCamera();
			break;
		case R.id.ib_camera_flash:
			cameraView.changeFlash();
			break;
		case R.id.ib_camera_grid:
			if (imgGrid.getVisibility() == View.VISIBLE) {
				imgGrid.setVisibility(View.GONE);
				ib_camera_grid
						.setBackgroundResource(R.drawable.camera_grid_normal);
				break;
			}
			ib_camera_grid.setBackgroundResource(R.drawable.camera_grid_press);
			imgGrid.setVisibility(View.VISIBLE);
			break;
		case R.id.ib_camera_take_picture:
			cameraView.takePicture(false);
			break;
		}
	}

	private int getScreenWidth() {
		getWindowManager().getDefaultDisplay().getMetrics(metric);
		return metric.widthPixels;
	}

	@Override
	public void onShake(int orientation) {
		// you can rotate views here
	}

	@SuppressLint("SdCardPath")
	@Override
	public void onTakePicture(boolean success, String filePath) {
		// sd/ResoCamera/(file)
		Log.i("111", "1111111");

		uiHandler.sendEmptyMessage(SAVE_SUCCEED);
	}

	@Override
	public void onChangeFlashMode(int flashMode) {
		switch (flashMode) {
		case CameraView.FLASH_AUTO:
			ib_camera_flash.setBackgroundResource(R.drawable.camera_flash_auto);
			break;
		case CameraView.FLASH_OFF:
			ib_camera_flash.setBackgroundResource(R.drawable.camera_flash_off);
			break;
		case CameraView.FLASH_ON:
			ib_camera_flash.setBackgroundResource(R.drawable.camera_flash_on);
			break;
		}
	}

	@Override
	public void onChangeCameraPosition(int camera_position) {

	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@SuppressLint("NewApi")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				Log.d("mark", "网络状态已经改变");
				connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				info = connectivityManager.getActiveNetworkInfo();
				if (info != null && info.isAvailable()) {
					String name = info.getTypeName();
					Log.d("mark", "当前网络名称：" + name);
				} else {
					Log.d("mark", "没有可用网络");
					Notification.Builder builder = new Notification.Builder(
							context);
					builder.setSmallIcon(R.drawable.ic_launcher)
							.setTicker("network error")
							.setAutoCancel(true)
							.setContentTitle("network error")
							// .setContentText("Event:"+temp.getTitle()+"REMAINING TIME："+timeleft/1000/60+"m")
							.setContentText("No network")
							.setSound(
									RingtoneManager
											.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
							.setLights(Color.GREEN, 0, 1);

					Notification notification = builder.getNotification();
					notificationManager.notify(1001, notification);
					Toast.makeText(context, "network error", 1000).show();

				}
			}
		}
	};
}
