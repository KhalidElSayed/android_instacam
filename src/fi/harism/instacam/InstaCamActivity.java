package fi.harism.instacam;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.MediaColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class InstaCamActivity extends Activity {

	private final InstaCamCamera mCamera = new InstaCamCamera();
	private InstaCamRS mInstaCamRS;
	private final ButtonObserver mObserverButton = new ButtonObserver();
	private final CameraObserver mObserverCamera = new CameraObserver();
	private final RendererObserver mObserverRenderer = new RendererObserver();
	private final SeekBarObserver mObserverSeekBar = new SeekBarObserver();
	private SharedPreferences mPreferences;

	private InstaCamRenderer mRenderer;
	private final InstaCamData mSharedData = new InstaCamData();

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mInstaCamRS = new InstaCamRS(this);

		mCamera.setCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
		mCamera.setSharedData(mSharedData);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

		this.overridePendingTransition(0, 0);

		setContentView(R.layout.instacam);
		mRenderer = (InstaCamRenderer) findViewById(R.id.instacam_renderer);
		mRenderer.setSharedData(mSharedData);
		mRenderer.setObserver(mObserverRenderer);

		View menu = findViewById(R.id.menu);
		menu.setVisibility(View.GONE);

		LayoutTransition layoutTransition = new LayoutTransition();
		layoutTransition.setDuration(250);
		ViewGroup root = (ViewGroup) findViewById(R.id.root);
		root.setLayoutTransition(layoutTransition);

		layoutTransition = new LayoutTransition();
		layoutTransition.setDuration(250);
		root = (ViewGroup) findViewById(R.id.footer);
		root.setLayoutTransition(layoutTransition);

		findViewById(R.id.button_exit).setOnClickListener(mObserverButton);
		findViewById(R.id.button_shoot).setOnClickListener(mObserverButton);
		findViewById(R.id.button_save).setOnClickListener(mObserverButton);
		findViewById(R.id.button_cancel).setOnClickListener(mObserverButton);
		findViewById(R.id.button_menu).setOnClickListener(mObserverButton);

		mPreferences = getPreferences(MODE_PRIVATE);

		final int SEEKBAR_IDS[][] = {
				{ R.id.seekbar_brightness, R.string.key_brightness, 5 },
				{ R.id.seekbar_contrast, R.string.key_contrast, 5 },
				{ R.id.seekbar_saturation, R.string.key_saturation, 8 },
				{ R.id.seekbar_corner_radius, R.string.key_corner_radius, 3 } };

		for (int ids[] : SEEKBAR_IDS) {
			SeekBar seekBar = (SeekBar) findViewById(ids[0]);
			seekBar.setOnSeekBarChangeListener(mObserverSeekBar);
			seekBar.setProgress(mPreferences.getInt(getString(ids[1]), ids[2]));
			if (seekBar.getProgress() == 0) {
				seekBar.setProgress(1);
				seekBar.setProgress(0);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onPause() {
		super.onPause();
		mCamera.onPause();
		mRenderer.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mCamera.onResume();
		mRenderer.onResume();
	}

	private final class ButtonObserver implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.button_exit:
				finish();
				break;
			case R.id.button_shoot:
				mCamera.autoFocus(mObserverCamera);
				break;
			case R.id.button_menu:
				View view = findViewById(R.id.menu);
				if (view.getVisibility() == View.GONE
						|| view.getVisibility() == View.INVISIBLE) {
					view.setVisibility(View.VISIBLE);
					view.bringToFront();
				} else {
					view.setVisibility(View.INVISIBLE);
				}
				break;
			case R.id.button_save:
				mSharedData.mImageProgress = ProgressDialog
						.show(InstaCamActivity.this, null,
								getString(R.string.saving));
				Thread thread = new Thread(new SaveRunnable());
				thread.setPriority(Thread.MAX_PRIORITY);
				thread.start();
				break;
			case R.id.button_cancel:
				mSharedData.mImageData = null;
				findViewById(R.id.button_shoot).setVisibility(View.VISIBLE);
				findViewById(R.id.buttons_cancel_save).setVisibility(View.GONE);
				mCamera.startPreview();
				break;
			}
		}

	}

	private final class CameraObserver implements Camera.ShutterCallback,
			Camera.AutoFocusCallback, Camera.PictureCallback {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			if (!success) {
				Toast.makeText(InstaCamActivity.this, R.string.focus_failed,
						Toast.LENGTH_SHORT).show();
			}
			camera.takePicture(this, null, this);
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			mSharedData.mImageData = data;

			Calendar calendar = Calendar.getInstance();
			mSharedData.mImageTime = calendar.getTimeInMillis();
		}

		@Override
		public void onShutter() {
			findViewById(R.id.buttons_cancel_save).setVisibility(View.VISIBLE);
			findViewById(R.id.button_shoot).setVisibility(View.GONE);
		}

	}

	private class RendererObserver implements InstaCamRenderer.Observer {
		@Override
		public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
			try {
				mCamera.setPreviewTexture(surfaceTexture);
			} catch (final Exception ex) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(InstaCamActivity.this, ex.getMessage(),
								Toast.LENGTH_LONG).show();
					}
				});
			}
		}
	}

	private final class SaveRunnable implements Runnable {

		@Override
		public void run() {
			String error = null;
			try {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(mSharedData.mImageTime);

				String pictureName = String.format(
						"InstaCam_%d%02d%02d_%02d%02d%02d",
						calendar.get(Calendar.YEAR),
						calendar.get(Calendar.MONTH) + (1 - Calendar.JANUARY),
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

				Bitmap bitmap = BitmapFactory.decodeByteArray(
						mSharedData.mImageData, 0,
						mSharedData.mImageData.length, options);

				mInstaCamRS.applyFilter(InstaCamActivity.this, bitmap,
						mSharedData);

				FileOutputStream fos = new FileOutputStream(filePath);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				fos.flush();
				fos.close();
				bitmap.recycle();

				ContentValues v = new ContentValues();
				v.put(MediaColumns.TITLE, pictureName);
				v.put(MediaColumns.DISPLAY_NAME, pictureName);
				v.put(ImageColumns.DESCRIPTION, "Taken with InstaCam.");
				v.put(MediaColumns.DATE_ADDED, calendar.getTimeInMillis());
				v.put(ImageColumns.DATE_TAKEN, calendar.getTimeInMillis());
				v.put(MediaColumns.DATE_MODIFIED, calendar.getTimeInMillis());
				v.put(MediaColumns.MIME_TYPE, "image/jpeg");
				v.put(ImageColumns.ORIENTATION,
						mCamera.getCameraInfo().orientation);
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
				error = ex.getMessage();
			}

			final String errorMsg = error;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mSharedData.mImageProgress.dismiss();
					mSharedData.mImageProgress = null;
					mSharedData.mImageData = null;
					findViewById(R.id.button_shoot).setVisibility(View.VISIBLE);
					findViewById(R.id.buttons_cancel_save).setVisibility(
							View.GONE);
					mCamera.startPreview();
					if (errorMsg != null) {
						Toast.makeText(InstaCamActivity.this, errorMsg,
								Toast.LENGTH_LONG).show();
					}
				}
			});
		}
	}

	private final class SeekBarObserver implements
			SeekBar.OnSeekBarChangeListener {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {

			switch (seekBar.getId()) {
			case R.id.seekbar_brightness: {
				mPreferences.edit()
						.putInt(getString(R.string.key_brightness), progress)
						.commit();
				mSharedData.mBrightness = (progress - 5) / 10f;

				TextView textView = (TextView) findViewById(R.id.text_brightness);
				textView.setText(getString(R.string.seekbar_brightness,
						progress - 5));
				break;
			}
			case R.id.seekbar_contrast: {
				mPreferences.edit()
						.putInt(getString(R.string.key_contrast), progress)
						.commit();
				mSharedData.mContrast = (progress - 5) / 10f;
				TextView textView = (TextView) findViewById(R.id.text_contrast);
				textView.setText(getString(R.string.seekbar_contrast,
						progress - 5));
				break;
			}
			case R.id.seekbar_saturation: {
				mPreferences.edit()
						.putInt(getString(R.string.key_saturation), progress)
						.commit();
				mSharedData.mSaturation = (progress - 8) / 8f;
				TextView textView = (TextView) findViewById(R.id.text_saturation);
				textView.setText(getString(R.string.seekbar_saturation,
						progress - 8));
				break;
			}
			case R.id.seekbar_corner_radius: {
				mPreferences
						.edit()
						.putInt(getString(R.string.key_corner_radius), progress)
						.commit();
				mSharedData.mCornerRadius = progress / 10f;
				TextView textView = (TextView) findViewById(R.id.text_corner_radius);
				textView.setText(getString(R.string.seekbar_corner_radius,
						-progress));
				break;
			}
			}
			mRenderer.requestRender();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}
	}

}