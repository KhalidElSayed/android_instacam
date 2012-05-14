package fi.harism.instacam;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import fi.harism.instacam.rs.ScriptC_filter;

public class FilterRS {

	private RenderScript mRS;
	private ScriptC_filter mScript;

	public FilterRS(Context context) {
		mRS = RenderScript.create(context);
		mScript = new ScriptC_filter(mRS, context.getResources(), R.raw.filter);
	}

	public void apply(Context context, Bitmap bitmap, float brightness,
			float contrast, float saturation) {
		Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		mScript.invoke_filterImpl(allocation, brightness, contrast, saturation);
		allocation.copyTo(bitmap);
		allocation.destroy();
	}
}
