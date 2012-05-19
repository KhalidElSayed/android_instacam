/*
   Copyright 2012 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.instacam;

import java.io.IOException;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.opengl.Matrix;

/**
 * Class for encapsulating Camera related functionality.
 */
public class InstaCamCamera {

	// Current Camera instance.
	private Camera mCamera;
	// Current Camera Id.
	private int mCameraId;
	// Current Camera CameraInfo.
	private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
	// SharedData instance.
	private InstaCamData mSharedData;
	// Surface texture instance.
	private SurfaceTexture mSurfaceTexture;

	public int getOrientation() {
		if (mCameraInfo == null || mSharedData == null) {
			return 0;
		}
		if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
			return (mCameraInfo.orientation - mSharedData.mOrientationDevice + 360) % 360;
		} else {
			return (mCameraInfo.orientation + mSharedData.mOrientationDevice) % 360;
		}
	}

	public boolean isCameraFront() {
		return mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT;
	}

	/**
	 * Must be called from Activity.onPause(). Stops preview and releases Camera
	 * instance.
	 */
	public void onPause() {
		mSurfaceTexture = null;
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	/**
	 * Should be called from Activity.onResume(). Recreates Camera instance.
	 */
	public void onResume() {
		openCamera();
	}

	/**
	 * Handles camera opening.
	 */
	private void openCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		if (mCameraId >= 0) {
			Camera.getCameraInfo(mCameraId, mCameraInfo);
			mCamera = Camera.open(mCameraId);
			// Disable jpeg rotation. We'll put it to EXIF data ourselves once
			// final picture is saved.
			Camera.Parameters params = mCamera.getParameters();
			params.setRotation(0);
			mCamera.setParameters(params);
			try {
				if (mSurfaceTexture != null) {
					mCamera.setPreviewTexture(mSurfaceTexture);
					mCamera.startPreview();
				}
			} catch (Exception ex) {
			}
		}

		updateRotation();
	}

	/**
	 * Selects either front-facing or back-facing camera.
	 */
	public void setCameraFront(boolean frontFacing) {
		int facing = frontFacing ? CameraInfo.CAMERA_FACING_FRONT
				: CameraInfo.CAMERA_FACING_BACK;

		mCameraId = -1;
		int numberOfCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numberOfCameras; ++i) {
			Camera.getCameraInfo(i, mCameraInfo);
			if (mCameraInfo.facing == facing) {
				mCameraId = i;
				break;
			}
		}

		openCamera();
	}

	/**
	 * Simply forwards call to Camera.setPreviewTexture.
	 */
	public void setPreviewTexture(SurfaceTexture surfaceTexture)
			throws IOException {
		mSurfaceTexture = surfaceTexture;
		mCamera.setPreviewTexture(surfaceTexture);
	}

	/**
	 * Setter for storing shared data.
	 */
	public void setSharedData(InstaCamData sharedData) {
		mSharedData = sharedData;
	}

	/**
	 * Simply forwards call to Camera.startPreview.
	 */
	public void startPreview() {
		mCamera.startPreview();
	}

	/**
	 * Simply forwards call to Camera.stopPreview.
	 */
	public void stopPreview() {
		mCamera.stopPreview();
	}

	/**
	 * Handles picture taking callbacks etc etc.
	 */
	public void takePicture(Observer observer) {
		mCamera.autoFocus(new CameraObserver(observer));
	}

	/**
	 * Updated rotation matrix, aspect ratio etc.
	 */
	public void updateRotation() {
		if (mCamera == null || mSharedData == null) {
			return;
		}

		int orientation = mCameraInfo.orientation;
		Matrix.setRotateM(mSharedData.mOrientationM, 0, orientation, 0f, 0f, 1f);

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

	/**
	 * Class for implementing Camera related callbacks.
	 */
	private final class CameraObserver implements Camera.ShutterCallback,
			Camera.AutoFocusCallback, Camera.PictureCallback {

		private Observer mObserver;

		private CameraObserver(Observer observer) {
			mObserver = observer;
		}

		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			mObserver.onAutoFocus(success);
			mCamera.takePicture(this, null, this);
		}

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			mObserver.onPictureTaken(data);
		}

		@Override
		public void onShutter() {
			mObserver.onShutter();
		}

	}

	/**
	 * Interface for observing picture taking process.
	 */
	public interface Observer {

		/**
		 * Called once auto focus is done.
		 */
		public void onAutoFocus(boolean success);

		/**
		 * Called once picture has been taken.
		 */
		public void onPictureTaken(byte[] jpeg);

		/**
		 * Called to notify about shutter event.
		 */
		public void onShutter();
	}

}
