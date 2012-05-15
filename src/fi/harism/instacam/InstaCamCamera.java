package fi.harism.instacam;

import java.io.IOException;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.Matrix;

public class InstaCamCamera {

	private Camera mCamera;
	private int mCameraId;
	private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
	private InstaCamData mSharedData;

	public void autoFocus(Camera.AutoFocusCallback callback) {
		mCamera.autoFocus(callback);
	}

	public Camera.CameraInfo getCameraInfo() {
		return mCameraInfo;
	}

	public void onPause() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public void onResume() {
		if (mCameraId >= 0) {
			Camera.getCameraInfo(mCameraId, mCameraInfo);
			mCamera = Camera.open(mCameraId);
		}

		if (mCamera != null && mSharedData != null) {
			int orientation = mCameraInfo.orientation;
			Matrix.setRotateM(mSharedData.mOrientationM, 0, orientation, 0f,
					0f, 1f);

			Camera.Size size = mCamera.getParameters().getPreviewSize();

			if (orientation % 90 == 0) {
				int w = size.width;
				size.width = size.height;
				size.height = w;
			}

			mSharedData.mAspectRatioPreview[0] = (float) Math.min(size.width,
					size.height) / size.width;
			mSharedData.mAspectRatioPreview[1] = (float) Math.min(size.width,
					size.height) / size.height;
		}

	}

	public void setCamera(int facing) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		mCameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; ++i) {
			Camera.getCameraInfo(i, mCameraInfo);
			if (mCameraInfo.facing == facing) {
				mCameraId = i;
				break;
			}
		}
	}

	public void setPreviewTexture(SurfaceTexture surfaceTexture)
			throws IOException {
		mCamera.setPreviewTexture(surfaceTexture);
		mCamera.startPreview();
	}

	public void setSharedData(InstaCamData sharedData) {
		mSharedData = sharedData;
	}

	public void startPreview() {
		mCamera.startPreview();
	}

}
