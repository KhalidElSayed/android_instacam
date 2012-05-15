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
				R.raw.filter);
	}

	public void applyFilter(Context context, Bitmap bitmap, InstaCamData data) {
		Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		mScript.invoke_filterImpl(allocation, data.mBrightness, data.mContrast,
				data.mSaturation);
		allocation.copyTo(bitmap);
		allocation.destroy();
	}
}
