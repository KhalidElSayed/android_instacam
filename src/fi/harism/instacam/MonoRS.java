package fi.harism.instacam;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import fi.harism.instacam.rs.ScriptC_mono;

public class MonoRS {

	private RenderScript mRS;
	private ScriptC_mono mScript;

	public MonoRS(Context context) {
		mRS = RenderScript.create(context);
		mScript = new ScriptC_mono(mRS, context.getResources(), R.raw.mono);
	}

	public void apply(Context context, Bitmap bitmap) {
		Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		mScript.invoke_monoImpl(allocation);
		allocation.copyTo(bitmap);
		allocation.destroy();
	}
}
