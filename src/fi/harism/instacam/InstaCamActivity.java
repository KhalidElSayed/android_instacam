package fi.harism.instacam;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class InstaCamActivity extends Activity implements
		Camera.PictureCallback {

	private Camera mCamera;
	private int mCameraId = -1;
	private MonoRS mMonoRS;
	private InstaCamRenderer mRenderer;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int numberOfCameras = Camera.getNumberOfCameras();
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		for (int i = 0; i < numberOfCameras; ++i) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				mCameraId = i;
			}
			if (mCameraId == -1
					&& cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				mCameraId = i;
			}
		}

		mMonoRS = new MonoRS(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		this.overridePendingTransition(0, 0);

		setContentView(R.layout.instacam);
		mRenderer = (InstaCamRenderer) findViewById(R.id.instacam_renderer);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
		mRenderer.onPause();

		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;

		mRenderer.setCamera(null, 0);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		try {
			Calendar calendar = Calendar.getInstance();

			String pictureName = String.format(
					"InstaCam_%d%02d%02d_%02d%02d%02d",
					calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)
							+ (1 - Calendar.JANUARY),
					calendar.get(Calendar.DATE),
					calendar.get(Calendar.HOUR_OF_DAY),
					calendar.get(Calendar.MINUTE),
					calendar.get(Calendar.SECOND));

			File filePath = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			filePath = new File(filePath, "/InstaCam/");
			filePath.mkdirs();
			filePath = new File(filePath, pictureName + ".jpeg");
			filePath.createNewFile();

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,
					options);

			mMonoRS.apply(this, bitmap);

			FileOutputStream fos = new FileOutputStream(filePath);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			fos.flush();
			fos.close();
			bitmap.recycle();

			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(mCameraId, cameraInfo);

			ContentValues v = new ContentValues();
			v.put(MediaColumns.TITLE, pictureName);
			v.put(MediaColumns.DISPLAY_NAME, pictureName);
			v.put(ImageColumns.DESCRIPTION, "Taken with InstaCam.");
			v.put(MediaColumns.DATE_ADDED, calendar.getTimeInMillis());
			v.put(ImageColumns.DATE_TAKEN, calendar.getTimeInMillis());
			v.put(MediaColumns.DATE_MODIFIED, calendar.getTimeInMillis());
			v.put(MediaColumns.MIME_TYPE, "image/jpeg");
			v.put(ImageColumns.ORIENTATION, cameraInfo.orientation);
			v.put(MediaColumns.DATA, filePath.getAbsolutePath());

			File parent = filePath.getParentFile();
			String path = parent.toString().toLowerCase();
			String name = parent.getName().toLowerCase();
			v.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
			v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
			v.put(MediaColumns.SIZE, filePath.length());

			// if( targ_loc != null ) {
			// v.put(Images.Media.LATITUDE, loc.getLatitude());
			// v.put(Images.Media.LONGITUDE, loc.getLongitude());
			// }

			ContentResolver c = getContentResolver();
			c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
		} catch (Exception ex) {
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		mRenderer.onResume();

		mCamera = Camera.open(mCameraId);

		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(mCameraId, cameraInfo);
		mRenderer.setCamera(mCamera, cameraInfo.orientation);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			mRenderer.takePicture(this);
			return true;
		}
		return super.onTouchEvent(me);
	}

}