package fi.harism.instacam;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.Toast;

public class InstaCamRenderer extends GLSurfaceView implements
		GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

	private final float mAspectRatio[] = new float[2];
	private final float mAspectRatioPreview[] = new float[2];
	private float mBrightness, mContrast, mSaturation;
	private Camera mCamera;
	private final InstaCamFbo mFboExternal = new InstaCamFbo();
	private final InstaCamFbo mFboOffscreen = new InstaCamFbo();
	private ByteBuffer mFullQuadVertices;
	private final float[] mOrientationM = new float[16];
	private final InstaCamShader mShaderCopyOes = new InstaCamShader();
	private final InstaCamShader mShaderFilter = new InstaCamShader();
	private SurfaceTexture mSurfaceTexture;
	private boolean mSurfaceTextureUpdate;
	private final float[] mTransformM = new float[16];

	private int mWidth, mHeight;

	public InstaCamRenderer(Context context) {
		super(context);
		init();
	}

	public InstaCamRenderer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		// Create full scene quad buffer.
		final byte FULL_QUAD_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
		mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
		mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);

		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	private String loadRawString(int rawId) throws Exception {
		InputStream is = getContext().getResources().openRawResource(rawId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = is.read(buf)) != -1) {
			baos.write(buf, 0, len);
		}
		return baos.toString();
	}

	@Override
	public synchronized void onDrawFrame(GL10 unused) {

		GLES20.glClearColor(.5f, .5f, .5f, 1f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		if (mCamera == null) {
			return;
		}

		if (mSurfaceTexture == null) {
			mSurfaceTexture = new SurfaceTexture(mFboExternal.getTexture(0));
			mSurfaceTexture.setOnFrameAvailableListener(this);
			try {
				mCamera.setPreviewTexture(mSurfaceTexture);
				mCamera.startPreview();
			} catch (Exception ex) {
				showError(ex.getMessage());
			}
		}

		if (mSurfaceTextureUpdate) {
			mSurfaceTexture.updateTexImage();
			mSurfaceTexture.getTransformMatrix(mTransformM);
			mSurfaceTextureUpdate = false;

			mFboOffscreen.bind();
			mFboOffscreen.bindTexture(0);

			mShaderCopyOes.useProgram();

			int uOrientationM = mShaderCopyOes.getHandle("uOrientationM");
			int uTransformM = mShaderCopyOes.getHandle("uTransformM");

			GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
			GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);

			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
					mFboExternal.getTexture(0));

			renderQuad(mShaderCopyOes.getHandle("aPosition"));
		}

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);

		mShaderFilter.useProgram();

		int uBrightness = mShaderFilter.getHandle("uBrightness");
		int uContrast = mShaderFilter.getHandle("uContrast");
		int uSaturation = mShaderFilter.getHandle("uSaturation");

		GLES20.glUniform1f(uBrightness, mBrightness);
		GLES20.glUniform1f(uContrast, mContrast);
		GLES20.glUniform1f(uSaturation, mSaturation);

		int uAspectRatio = mShaderFilter.getHandle("uAspectRatio");
		int uAspectRatioPreview = mShaderFilter
				.getHandle("uAspectRatioPreview");

		GLES20.glUniform2fv(uAspectRatio, 1, mAspectRatio, 0);
		GLES20.glUniform2fv(uAspectRatioPreview, 1, mAspectRatioPreview, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboOffscreen.getTexture(0));

		renderQuad(mShaderCopyOes.getHandle("aPosition"));
	}

	@Override
	public synchronized void onFrameAvailable(SurfaceTexture surfaceTexture) {
		mSurfaceTextureUpdate = true;
		requestRender();
	}

	@Override
	public synchronized void onSurfaceChanged(GL10 unused, int width, int height) {
		mWidth = width;
		mHeight = height;

		mAspectRatio[0] = (float) Math.min(mWidth, mHeight) / mWidth;
		mAspectRatio[1] = (float) Math.min(mWidth, mHeight) / mHeight;

		if (mSurfaceTexture != null) {
			mSurfaceTexture.release();
			mSurfaceTexture = null;
		}

		mFboExternal.reset();
		mFboExternal.init(mWidth, mHeight, 1, true);
		mFboOffscreen.reset();
		mFboOffscreen.init(mWidth, mHeight, 1, false);
	}

	@Override
	public synchronized void onSurfaceCreated(GL10 unused, EGLConfig config) {
		try {
			String vertexSource, fragmentSource;
			vertexSource = loadRawString(R.raw.copy_oes_vs);
			fragmentSource = loadRawString(R.raw.copy_oes_fs);
			mShaderCopyOes.deleteProgram();
			mShaderCopyOes.setProgram(vertexSource, fragmentSource);
			vertexSource = loadRawString(R.raw.filter_vs);
			fragmentSource = loadRawString(R.raw.filter_fs);
			mShaderFilter.setProgram(vertexSource, fragmentSource);

		} catch (Exception ex) {
			mShaderCopyOes.deleteProgram();
			mShaderFilter.deleteProgram();
			showError(ex.getMessage());
		}
	}

	private void renderQuad(int aPosition) {
		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mFullQuadVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	}

	public synchronized void setCamera(Camera camera, int orientation) {
		mCamera = camera;
		Matrix.setRotateM(mOrientationM, 0, orientation, 0f, 0f, 1f);

		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			Camera.Size size = params.getPreviewSize();

			if (orientation % 90 == 0) {
				int w = size.width;
				size.width = size.height;
				size.height = w;
			}

			mAspectRatioPreview[0] = (float) Math.min(size.width, size.height)
					/ size.width;
			mAspectRatioPreview[1] = (float) Math.min(size.width, size.height)
					/ size.height;

			requestRender();
		}

		if (camera == null && mSurfaceTexture != null) {
			mSurfaceTexture.release();
			mSurfaceTexture = null;
		}
	}

	public void setFilterValues(float brightness, float contrast,
			float saturation) {
		mBrightness = brightness;
		mContrast = contrast;
		mSaturation = saturation;
	}

	private synchronized void showError(final String errorMsg) {
		Handler handler = new Handler(getContext().getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	public synchronized void takePicture(final Camera.PictureCallback callback) {
		if (mCamera == null) {
			showError("Camera not initialized.");
			return;
		}
		mCamera.autoFocus(new Camera.AutoFocusCallback() {
			@Override
			public void onAutoFocus(boolean success, Camera camera) {
				if (!success) {
					showError("Auto focus failed.");
					return;
				}
				mCamera.takePicture(new Camera.ShutterCallback() {
					@Override
					public void onShutter() {
					}
				}, null, new Camera.PictureCallback() {
					@Override
					public void onPictureTaken(byte[] data, Camera camera) {
						callback.onPictureTaken(data, camera);
						camera.startPreview();
					}
				});
			}
		});
	}

}
