package fi.harism.instacam;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.Toast;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public class InstaCamRenderer extends GLSurfaceView implements
		GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

	private Camera mCamera;
	private final InstaCamShader mShaderCopyOes = new InstaCamShader();
	private final InstaCamFbo mFboExternal = new InstaCamFbo();
	private final InstaCamFbo mFboOffscreen = new InstaCamFbo();
	private ByteBuffer mFullQuadVertices;
	private SurfaceTexture mSurfaceTexture;
	private boolean mSurfaceTextureUpdate;
	private int mWidth, mHeight;
	private final float[] mTransformM = new float[16];
	private final float[] mOrientationM = new float[16];

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
	
	private synchronized String loadRawString(int rawId) throws Exception {
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
		}

		// Copy FBO to screen buffer.
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glViewport(0, 0, mWidth, mHeight);

		mShaderCopyOes.useProgram();
		int uOrientationM = mShaderCopyOes.getHandle("uOrientationM");
		int uTransformM = mShaderCopyOes.getHandle("uTransformM");
		int aPosition = mShaderCopyOes.getHandle("aPosition");

		GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
		GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFboExternal.getTexture(0));

		GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0,
				mFullQuadVertices);
		GLES20.glEnableVertexAttribArray(aPosition);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
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

		if (mSurfaceTexture != null) {
			mSurfaceTexture.release();
			mSurfaceTexture = null;
		}

		mFboExternal.reset();
		mFboExternal.init(mWidth, mHeight, 1, true);
	}

	@Override
	public synchronized void onSurfaceCreated(GL10 unused, EGLConfig config) {
		String vertexSource, fragmentSource;
		try {
			vertexSource = loadRawString(R.raw.copy_oes_vs);
			fragmentSource = loadRawString(R.raw.copy_oes_fs);
			mShaderCopyOes.deleteProgram();
			mShaderCopyOes.setProgram(vertexSource, fragmentSource);
		} catch (Exception ex) {
			mShaderCopyOes.deleteProgram();
			showError(ex.getMessage());
		}
	}

	public synchronized void setCamera(int cameraId, int orientation) {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		if (cameraId >= 0) {
			Matrix.setRotateM(mOrientationM, 0, orientation, 0f, 0f, 1f);
			mCamera = Camera.open(cameraId);
		} else {
			mSurfaceTexture.release();
			mSurfaceTexture = null;
		}
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

	public void takePicture(final Camera.PictureCallback callback) {
		if (mCamera != null) {
			mCamera.autoFocus(new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					if (success) {
						mCamera.takePicture(new Camera.ShutterCallback() {
							@Override
							public void onShutter() {
							}
						}, null, new Camera.PictureCallback() {
							@Override
							public void onPictureTaken(byte[] data,
									Camera camera) {
								callback.onPictureTaken(data, camera);
								camera.startPreview();
							}
						});
					} else {
						showError("Auto focus failed.");
					}
				}
			});
		} else {
			showError("Camera not initialized.");
		}
	}

}
