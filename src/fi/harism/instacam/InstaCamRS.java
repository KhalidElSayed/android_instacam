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

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import fi.harism.instacam.rs.ScriptC_instacam;

public class InstaCamRS {

	private RenderScript mRS;
	private ScriptC_instacam mScript;

	public InstaCamRS(Context context) {
		mRS = RenderScript.create(context);
		mScript = new ScriptC_instacam(mRS, context.getResources(),
				R.raw.instacam);
	}

	public void applyFilter(Context context, Bitmap bitmap, InstaCamData data) {
		Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		mScript.invoke_filterImpl(allocation, data.mBrightness, data.mContrast,
				data.mSaturation, data.mCornerRadius);
		allocation.copyTo(bitmap);
		allocation.destroy();
	}
}
