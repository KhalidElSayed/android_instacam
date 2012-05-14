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
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

public class InstaCamActivity extends Activity implements
		Camera.PictureCallback {

	private int mDefaultCameraId = -1;
	private InstaCamRenderer mRenderer;
	private MonoRS mMonoRS;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int numberOfCameras = Camera.getNumberOfCameras();
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; ++i) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				mDefaultCameraId = i;
			}
			if (mDefaultCameraId == -1
					&& cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
				mDefaultCameraId = i;
			}
		}
		
		mMonoRS = new MonoRS(this);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		
		this.overridePendingTransition(0, 0);
		
		setContentView(R.layout.instacam);
		mRenderer = (InstaCamRenderer)findViewById(R.id.instacam_renderer);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		if (me.getAction() == MotionEvent.ACTION_DOWN) {
			mRenderer.takePicture(this);
			return true;
		}
		return super.onTouchEvent(me);
	}

	@Override
	public void onResume() {
		super.onResume();
		mRenderer.onResume();

		CameraInfo cameraInfo = new CameraInfo();
		Camera.getCameraInfo(mDefaultCameraId, cameraInfo);
		mRenderer.setCamera(mDefaultCameraId, cameraInfo.orientation);
	}

	@Override
	public void onPause() {
		super.onPause();
		mRenderer.onPause();
		mRenderer.setCamera(-1, 0);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		try {
			Calendar calendar = Calendar.getInstance();

			String pictureName = String.format("InstaCam_%d%02d%02d_%02d%02d%02d",
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
			
			Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
			
			mMonoRS.apply(this, bitmap);
			
			FileOutputStream fos = new FileOutputStream(filePath);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
			fos.flush();
			fos.close();
			bitmap.recycle();

			ContentValues v = new ContentValues();
			v.put(Images.Media.TITLE, pictureName);
			v.put(Images.Media.DISPLAY_NAME, pictureName);
			v.put(Images.Media.DESCRIPTION, "Taken with InstaCam.");
			v.put(Images.Media.DATE_ADDED, calendar.getTimeInMillis());
			v.put(Images.Media.DATE_TAKEN, calendar.getTimeInMillis());
			v.put(Images.Media.DATE_MODIFIED, calendar.getTimeInMillis());
			v.put(Images.Media.MIME_TYPE, "image/jpeg");
			v.put(Images.Media.ORIENTATION, 90);
			v.put(Images.Media.DATA, filePath.getAbsolutePath());

			File parent = filePath.getParentFile();
			String path = parent.toString().toLowerCase();
			String name = parent.getName().toLowerCase();
			v.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
			v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
			v.put(Images.Media.SIZE, filePath.length());

			// if( targ_loc != null ) {
			// v.put(Images.Media.LATITUDE, loc.getLatitude());
			// v.put(Images.Media.LONGITUDE, loc.getLongitude());
			// }

			ContentResolver c = getContentResolver();
			c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
		} catch (Exception ex) {
		}
	}

}