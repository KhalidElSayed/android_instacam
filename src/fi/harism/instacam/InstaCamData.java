package fi.harism.instacam;

import android.app.ProgressDialog;

public class InstaCamData {
	public final float mAspectRatioPreview[] = new float[2];
	public float mBrightness, mContrast, mSaturation;

	public byte[] mImageData;
	public ProgressDialog mImageProgress;
	public long mImageTime;

	public final float mOrientationM[] = new float[16];
}
