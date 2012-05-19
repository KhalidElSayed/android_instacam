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

import android.app.ProgressDialog;

/**
 * Holder class for application wide data.
 */
public class InstaCamData {
	// Preview view aspect ration.
	public final float mAspectRatioPreview[] = new float[2];
	// Filter values.
	public float mBrightness, mContrast, mSaturation, mCornerRadius;
	// Predefined filter.
	public int mFilter;
	// Taken picture data (jpeg).
	public byte[] mImageData;
	// Progress dialog while saving picture.
	public ProgressDialog mImageProgress;
	// Picture capture time.
	public long mImageTime;
	// Device orientation degree.
	public int mOrientationDevice;
	// Camera orientation matrix.
	public final float mOrientationM[] = new float[16];
}
