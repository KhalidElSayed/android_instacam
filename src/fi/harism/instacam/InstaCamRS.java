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

/**
 * RenderScript container class.
 */
public class InstaCamRS {

	// Filter scripts.
	private ScriptC_filter_ansel mFilterAnsel;
	private ScriptC_filter_blackandwhite mFilterBlackAndWhite;
	private ScriptC_filter_cartoon mFilterCartoon;
	private ScriptC_filter_default mFilterDefault;
	private ScriptC_filter_georgia mFilterGeorgia;
	private ScriptC_filter_polaroid mFilterPolaroid;
	private ScriptC_filter_retro mFilterRetro;
	private ScriptC_filter_sahara mFilterSahara;
	private ScriptC_filter_sepia mFilterSepia;
	// RenderScript instance.
	private RenderScript mRS;

	/**
	 * Default constructor.
	 */
	public InstaCamRS(Context context) {
		mRS = RenderScript.create(context);
		mFilterAnsel = new ScriptC_filter_ansel(mRS, context.getResources(),
				R.raw.filter_ansel);
		mFilterBlackAndWhite = new ScriptC_filter_blackandwhite(mRS,
				context.getResources(), R.raw.filter_blackandwhite);
		mFilterCartoon = new ScriptC_filter_cartoon(mRS,
				context.getResources(), R.raw.filter_cartoon);
		mFilterGeorgia = new ScriptC_filter_georgia(mRS,
				context.getResources(), R.raw.filter_georgia);
		mFilterPolaroid = new ScriptC_filter_polaroid(mRS,
				context.getResources(), R.raw.filter_polaroid);
		mFilterRetro = new ScriptC_filter_retro(mRS, context.getResources(),
				R.raw.filter_retro);
		mFilterSahara = new ScriptC_filter_sahara(mRS, context.getResources(),
				R.raw.filter_sahara);
		mFilterSepia = new ScriptC_filter_sepia(mRS, context.getResources(),
				R.raw.filter_sepia);
		mFilterDefault = new ScriptC_filter_default(mRS,
				context.getResources(), R.raw.filter_default);
	}

	/**
	 * Applies filter from data values for given Bitmap.
	 */
	public void applyFilter(Bitmap bitmap, InstaCamData data) {
		// Generate allocation from Bitmap.
		Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);

		// Apply filter if one selected.
		switch (data.mFilter) {
		case 1:
			mFilterBlackAndWhite.forEach_root(allocation);
			break;
		case 2:
			mFilterAnsel.forEach_root(allocation);
			break;
		case 3:
			mFilterSepia.forEach_root(allocation);
			break;
		case 4:
			mFilterRetro.forEach_root(allocation);
			break;
		case 5:
			mFilterGeorgia.forEach_root(allocation);
			break;
		case 6:
			mFilterSahara.forEach_root(allocation);
			break;
		case 7:
			mFilterPolaroid.forEach_root(allocation);
			break;
		case 8:
			mFilterCartoon.forEach_root(allocation);
			break;
		}

		// Apply brightness, contrast and saturation.
		mFilterDefault.invoke_setBrightness(data.mBrightness);
		mFilterDefault.invoke_setContrast(data.mContrast);
		mFilterDefault.invoke_setSaturation(data.mSaturation);
		mFilterDefault.invoke_setCornerRadius(data.mCornerRadius);
		mFilterDefault.invoke_setSize(bitmap.getWidth(), bitmap.getHeight());
		mFilterDefault.forEach_root(allocation);

		// Copy allocation values back to Bitmap.
		allocation.copyTo(bitmap);
		allocation.destroy();
	}
}
