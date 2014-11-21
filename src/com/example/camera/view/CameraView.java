package com.example.camera.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CameraView implements SurfaceHolder.Callback,
		Camera.PictureCallback, Camera.AutoFocusCallback, View.OnTouchListener,
		ShakeListener.OnShakeListener {

	public final static String TAG = CameraView.class.getSimpleName();

	public final static int FLASH_AUTO = 2;
	public final static int FLASH_OFF = 0;
	public final static int FLASH_ON = 1;

	public static final int MODE4T3 = 43;
	public static final int MODE16T9 = 169;

	private int currentMODE = MODE4T3;
	private String locate = "δ֪λ��";
	private String nowTime = "δ֪ʱ��";

	private SurfaceHolder mHolder;
	private Camera mCamera;
	private CameraSizeComparator sizeComparator = new CameraSizeComparator();
	private FocusView focusView;

	private int flash_type = FLASH_AUTO; // 0 close , 1 open , 2 auto
	private static int camera_position = Camera.CameraInfo.CAMERA_FACING_BACK;// 0
																				// back
																				// camera
																				// ,
																				// 1
																				// front
																				// camera
	private int takePhotoOrientation = 90;

	private boolean isSquare;

	private int topDistance;
	private int bottonDistance;
	private int zoomFlag = 0;

	private SurfaceView surfaceView;
	@SuppressLint("SdCardPath")
	public String PATH_DIR = Environment.getExternalStorageDirectory()
			+ "/sdcard/MyCamera/";
	private String PATH_FILE;

	private String dirPath;
	private int screenDpi;
	private float focusAreaSize = 100;
	private OnCameraSelectListener onCameraSelectListener;

	public void setOnCameraSelectListener(
			OnCameraSelectListener onCameraSelectListener) {
		this.onCameraSelectListener = onCameraSelectListener;
	}

	public void setFocusAreaSize(float focusAreaSize) {
		this.focusAreaSize = focusAreaSize;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	private Context context;

	public CameraView(Context context) {
		this.context = context;
	}

	/**
	 * @param surfaceView
	 *            the camera view you should give it
	 * @param screenWidth
	 *            width of the screen
	 * @param cameraMode
	 *            set the camera preview proportion ,default is MODE4T3;
	 *            {@link #MODE4T3}
	 * @throws Exception
	 */
	public void setCameraView(SurfaceView surfaceView, int screenWidth,
			int cameraMode) throws Exception {
		this.surfaceView = surfaceView;
		this.currentMODE = cameraMode;
		if (currentMODE == MODE4T3) {
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.surfaceView
					.getLayoutParams();
			layoutParams.width = screenWidth;
			layoutParams.height = screenWidth * 4 / 3;
			this.surfaceView.setLayoutParams(layoutParams);
		} else if (currentMODE == MODE16T9) {
			ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) this.surfaceView
					.getLayoutParams();
			layoutParams.width = screenWidth;
			layoutParams.height = screenWidth * 16 / 9;
			this.surfaceView.setLayoutParams(layoutParams);
		}
		ShakeListener.newInstance().setOnShakeListener(this);
		screenDpi = context.getResources().getDisplayMetrics().densityDpi;
		mHolder = surfaceView.getHolder();
		surfaceView.setOnTouchListener(this);
		mHolder.addCallback(this);
		mHolder.setKeepScreenOn(true);
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * set camera top distance
	 * 
	 * @param topDistance
	 */
	public void setTopDistance(int topDistance) {
		this.topDistance = topDistance;
	}

	public void setBottonDistance(int bottonDistance) {
		this.bottonDistance = bottonDistance;
	}

	public void setFocusView(FocusView focusView) {
		this.focusView = focusView;
	}

	public void setLocate(String locate) {
		this.locate = locate;
	}

	public void setData(String data) {
		this.nowTime = data;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (screenDpi == DisplayMetrics.DENSITY_HIGH) {
			zoomFlag = 10;
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mHolder = holder;
		mHolder.setKeepScreenOn(true);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		closeCamera();
	}

	@SuppressLint("NewApi")
	private void openCamera() {
		try {
			closeCamera();
			mCamera = Camera.open(camera_position);
			mCamera.setDisplayOrientation(90);
			mCamera.setPreviewDisplay(mHolder);
			setCameraPictureSize();
			setCameraPreviewSize();
			changeFlash(flash_type);
			mCamera.startPreview();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void closeCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
		}
		flash_type = FLASH_AUTO;
		mCamera = null;
	}

	private void resetCamera() {
		if (onCameraSelectListener != null) {
			onCameraSelectListener.onChangeCameraPosition(camera_position);
		}
		Log.i(TAG, "camera-camera-position:" + camera_position);
		closeCamera();
		openCamera();
	}

	private void setCameraPreviewSize() {
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPreviewSizes();
		Collections.sort(sizes, sizeComparator);
		for (Camera.Size size : sizes) {
			params.setPreviewSize(size.width, size.height);
			if (size.width * 1.0 / size.height * 1.0 == 4.0 / 3.0
					&& currentMODE == MODE4T3) {
				break;
			} else if (size.width * 1.0 / size.height * 1.0 == 16.0 / 9.0
					&& currentMODE == MODE16T9) {
				break;
			}
		}
		mCamera.setParameters(params);
	}

	private void setCameraPictureSize() {
		Camera.Parameters params = mCamera.getParameters();
		List<Camera.Size> sizes = params.getSupportedPictureSizes();
		Collections.sort(sizes, sizeComparator);
		for (Camera.Size size : sizes) {
			params.setPictureSize(size.width, size.height);
			if (size.width * 1.0 / size.height * 1.0 == 4.0 / 3.0
					&& currentMODE == MODE4T3 && size.height < 2500) {
				break;
			} else if (size.width * 1.0 / size.height * 1.0 == 16.0 / 9.0
					&& currentMODE == MODE16T9 && size.height < 2500) {
				break;
			}
		}
		params.setJpegQuality(100);
		params.setPictureFormat(ImageFormat.JPEG);
		mCamera.setParameters(params);
	}

	/**
	 * use with activity or fragment life circle
	 */
	public final void onResume() {
		Log.i(TAG, "camera-resume");
		if (surfaceView == null)
			throw new NullPointerException(
					"not init surfaceView for camera view");
		openCamera();
		ShakeListener.newInstance().start(context);
	}

	/**
	 * seem to onResume {@link #onResume()}
	 */
	public final void onPause() {
		Log.i(TAG, "camera-pause");
		closeCamera();
		ShakeListener.newInstance().stop();
	}

	public final int changeFlash(int flash_type) {
		this.flash_type = flash_type;
		return changeFlash();
	}

	/**
	 * change camera flash mode
	 */
	public final int changeFlash() {
		if (mCamera == null) {
			return -1;
		}
		Camera.Parameters parameters = mCamera.getParameters();
		List<String> FlashModes = parameters.getSupportedFlashModes();
		if (FlashModes == null) {
			return 0;
		}
		if (onCameraSelectListener != null) {
			onCameraSelectListener.onChangeFlashMode((flash_type) % 3);
		}
		Log.i(TAG, "camera-flash-type:" + flash_type);
		switch (flash_type % 3) {
		case FLASH_ON:
			if (FlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
				flash_type++;
				mCamera.setParameters(parameters);
			}
			break;
		case FLASH_OFF:
			if (FlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				flash_type++;
				mCamera.setParameters(parameters);
			}
			break;
		case FLASH_AUTO:
			if (FlashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
				flash_type++;
				mCamera.setParameters(parameters);
			}
			break;
		default:
			if (FlashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
				parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
				flash_type++;
				mCamera.setParameters(parameters);
			}
			break;
		}
		return flash_type;
	}

	/**
	 * change camera facing
	 */
	@SuppressLint("NewApi")
	public final int changeCamera() {
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		int cameraCount = Camera.getNumberOfCameras();
		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (camera_position == Camera.CameraInfo.CAMERA_FACING_BACK) {
				camera_position = Camera.CameraInfo.CAMERA_FACING_FRONT;
				resetCamera();
				return camera_position;
			} else if (camera_position == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				camera_position = Camera.CameraInfo.CAMERA_FACING_BACK;
				resetCamera();
				return camera_position;
			}
		}
		return camera_position;
	}

	public final void takePicture(boolean isSquare) {
		if (mCamera != null) {
			this.isSquare = isSquare;
			mCamera.takePicture(null, null, this);
		}
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void onPictureTaken(final byte[] data, final Camera camera) {
		try {
			if (dirPath != null && !dirPath.equals("")) {
				PATH_DIR = dirPath;
			}
			long time = System.currentTimeMillis();
			PATH_FILE = PATH_DIR + "IMG_" + time + ".jpg";
			Date d = new Date(time);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			nowTime = sdf.format(d);
			createFolder(PATH_DIR);
			createFile(PATH_FILE);
			new Thread(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0,
							data.length);
					/**
					 * ���ˮӡ
					 */
					Bitmap tempBitmap;
					Canvas canvasTemp;
					int width = bitmap.getWidth();
					int height = bitmap.getHeight();
					tempBitmap = Bitmap.createBitmap(width, height,
							Config.RGB_565);
					canvasTemp = new Canvas(tempBitmap);
					Paint paint = new Paint(); // ��������
					paint.setDither(true);
					paint.setFilterBitmap(true);
					Rect src = new Rect(0, 0, width, height);
					Rect dst = new Rect(0, 0, width, height);
					canvasTemp.drawBitmap(bitmap, src, dst, paint);

					Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG
							| Paint.DEV_KERN_TEXT_FLAG);
					textPaint.setTextSize(100.0f);
					textPaint.setTypeface(Typeface.DEFAULT_BOLD); // ����Ĭ�ϵĿ��
					textPaint.setColor(Color.WHITE);
					
					Path pathLocate = new Path(); // ����һ��·��
		            pathLocate.moveTo(width-200, height-100); 
		            pathLocate.lineTo(width-200,100);
					canvasTemp.drawTextOnPath("�ص㣺"+locate, pathLocate, 0, 0,
							textPaint);
					
					Path pathData = new Path(); // ����һ��·��
					pathData.moveTo(width-50, height-100); 
					pathData.lineTo(width-50,100);
					canvasTemp.drawTextOnPath("ʱ�䣺"+nowTime, pathData, 0, 0,
							textPaint);
					
					canvasTemp.save(Canvas.ALL_SAVE_FLAG);
					canvasTemp.restore();
					bitmap = tempBitmap;

					FileOutputStream fos;
					Matrix matrix = new Matrix();
					matrix.postRotate(takePhotoOrientation);
					if (camera_position == Camera.CameraInfo.CAMERA_FACING_FRONT) {
						matrix.postScale(1, -1);
					}
					if (isSquare) {
						bitmap = Bitmap.createBitmap(bitmap, 0, 0,
								bitmap.getHeight(), bitmap.getHeight(), matrix,
								true);
					} else {
						bitmap = Bitmap.createBitmap(bitmap, 0, 0,
								bitmap.getWidth(), bitmap.getHeight(), matrix,
								true);
					}
					System.gc();
					try {
						fos = new FileOutputStream(PATH_FILE);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos);

						bos.flush();
						bos.close();
						bitmap.recycle();

						context.sendBroadcast(new Intent(
								Intent.ACTION_MEDIA_MOUNTED, Uri
										.parse("file://" + PATH_DIR)));
						if (onCameraSelectListener != null) {
							onCameraSelectListener.onTakePicture(true,
									PATH_FILE);
						}

						openCamera();
					} catch (Exception e) {
						bitmap.recycle();
						e.printStackTrace();
						if (onCameraSelectListener != null) {
							onCameraSelectListener.onTakePicture(false, null);
						}
					}
					System.gc();
				}
			}).start();
			closeCamera();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		if (focusView != null) {
			focusView.clearDraw();
		}
	}

	/**
	 * Convert touch position x:y in (-1000~1000)
	 */
	private Rect calculateTapArea(float x, float y, float coefficient) {
		int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
		x = x / surfaceView.getWidth();
		y = y / surfaceView.getHeight();

		float cameraX = y;
		float cameraY = 1 - x;

		int centerX = (int) (cameraX * 2000 - 1000);
		int centerY = (int) (cameraY * 2000 - 1000);
		int left = clamp(centerX - areaSize / 2, -1000, 1000);
		int top = clamp(centerY - areaSize / 2, -1000, 1000);
		int right = clamp(left + areaSize, -1000, 1000);
		int bottom = clamp(top + areaSize, -1000, 1000);

		return new Rect(left, top, right, bottom);
	}

	private int clamp(int x, int min, int max) {
		if (x > max) {
			return max;
		}
		if (x < min) {
			return min;
		}
		return x;
	}

	private float mDist;

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (mCamera == null) {
			return false;
		}
		Camera.Parameters params = mCamera.getParameters();
		int action = event.getAction();

		if (event.getPointerCount() > 1) {
			if (action == MotionEvent.ACTION_POINTER_DOWN) {
				mDist = getFingerSpacing(event);
			} else if (action == MotionEvent.ACTION_MOVE
					&& params.isZoomSupported()) {
				mCamera.cancelAutoFocus();
				handleZoom(event, params);
			}
			if (focusView != null) {
				focusView.clearDraw();
			}
		} else {
			if (action == MotionEvent.ACTION_DOWN) {
				if (focusView != null) {
					focusView.clearDraw();
					focusView.drawLine(event.getRawX(), event.getRawY()
							- topDistance - bottonDistance);
				}
			}
			if (action == MotionEvent.ACTION_UP) {
				handleFocus(event);
			}
		}
		return true;
	}

	private void handleZoom(MotionEvent event, Camera.Parameters params) {
		int maxZoom = params.getMaxZoom();
		int zoom = params.getZoom();
		float newDist = getFingerSpacing(event);
		if (newDist > mDist && newDist - mDist > zoomFlag) {
			if (zoom < maxZoom)
				zoom++;
		} else if (newDist < mDist && mDist - newDist > zoomFlag) {
			if (zoom > 0)
				zoom--;
		}
		mDist = newDist;
		params.setZoom(zoom);
		mCamera.setParameters(params);
	}

	@SuppressLint("NewApi")
	public void handleFocus(MotionEvent event) {
		if (mCamera != null) {
			mCamera.cancelAutoFocus();

			Rect focusRect = calculateTapArea(event.getRawX(), event.getRawY()
					- topDistance - bottonDistance, 1f);
			Rect meteringRect = calculateTapArea(event.getRawX(),
					event.getRawY() - topDistance - bottonDistance, 2f);

			Camera.Parameters parameters = mCamera.getParameters();
			if (parameters.getSupportedFocusModes().contains(
					Camera.Parameters.FOCUS_MODE_AUTO))
				parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

			if (parameters.getMaxNumFocusAreas() > 0) {
				List<Camera.Area> areaList = new ArrayList<Camera.Area>();
				areaList.add(new Camera.Area(focusRect, 1000));
				parameters.setFocusAreas(areaList);
			}

			if (parameters.getMaxNumMeteringAreas() > 0) {
				List<Camera.Area> meteringList = new ArrayList<Camera.Area>();
				meteringList.add(new Camera.Area(meteringRect, 1000));
				parameters.setMeteringAreas(meteringList);
			}

			mCamera.setParameters(parameters);
			mCamera.autoFocus(this);
		}
	}

	private float getFingerSpacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	private void createParentFolder(File file) throws Exception {
		if (!file.getParentFile().exists()) {
			if (!file.getParentFile().mkdirs()) {
				throw new Exception("create parent directory failure!");
			}
		}
	}

	private void createFolder(String path) throws Exception {
		path = separatorReplace(path);
		File folder = new File(path);
		if (folder.isDirectory()) {
			return;
		} else if (folder.isFile()) {
			deleteFile(path);
		}
		folder.mkdirs();
	}

	private File createFile(String path) throws Exception {
		path = separatorReplace(path);
		File file = new File(path);
		if (file.isFile()) {
			return file;
		} else if (file.isDirectory()) {
			deleteFolder(path);
		}
		return createFile(file);
	}

	private File createFile(File file) throws Exception {
		createParentFolder(file);
		if (!file.createNewFile()) {
			throw new Exception("create file failure!");
		}
		return file;
	}

	private void deleteFolder(String path) throws Exception {
		path = separatorReplace(path);
		File folder = getFolder(path);
		File[] files = folder.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteFolder(file.getAbsolutePath());
			} else if (file.isFile()) {
				deleteFile(file.getAbsolutePath());
			}
		}
		folder.delete();
	}

	private File getFolder(String path) throws FileNotFoundException {
		path = separatorReplace(path);
		File folder = new File(path);
		if (!folder.isDirectory()) {
			throw new FileNotFoundException("folder not found!");
		}
		return folder;
	}

	private String separatorReplace(String path) {
		return path.replace("\\", "/");
	}

	private void deleteFile(String path) throws Exception {
		path = separatorReplace(path);
		File file = getFile(path);
		if (!file.delete()) {
			throw new Exception("delete file failure");
		}
	}

	private File getFile(String path) throws FileNotFoundException {
		path = separatorReplace(path);
		File file = new File(path);
		if (!file.isFile()) {
			throw new FileNotFoundException("file not found!");
		}
		return file;
	}

	@Override
	public void onShake(int orientation) {
		if (onCameraSelectListener != null) {
			onCameraSelectListener.onShake(orientation);
		}
		this.takePhotoOrientation = orientation;
	}

	public interface OnCameraSelectListener {
		public void onTakePicture(boolean success, String filePath);

		public void onChangeFlashMode(int flashMode);

		public void onChangeCameraPosition(int camera_position);

		public void onShake(int orientation);
	}

	public static class CameraSizeComparator implements Comparator<Camera.Size> {
		public int compare(Camera.Size lhs, Camera.Size rhs) {
			if (lhs.width == rhs.width) {
				return 0;
			} else if (lhs.width < rhs.width) {
				return 1;
			} else {
				return -1;
			}
		}
	}
}