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

	/**
	 * Simply forwards call to underlying Camera.autoFocus.
	 */
	public void autoFocus(Camera.AutoFocusCallback callback) {
		mCamera.autoFocus(callback);
	}

	/**
	 * Getter for current Camera CameraInfo.
	 */
	public Camera.CameraInfo getCameraInfo() {
		return mCameraInfo;
	}

	/**
	 * Must be called from Activity.onPause(). Stops preview and releases Camera
	 * instance.
	 */
	public void onPause() {
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

	/**
	 * Selects either front-facing or back-facing camera.
	 */
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

	/**
	 * Simply forwards call to Camera.setPreviewTexture.
	 */
	public void setPreviewTexture(SurfaceTexture surfaceTexture)
			throws IOException {
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

}
