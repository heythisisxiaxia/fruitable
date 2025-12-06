package com.example.fruitable;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;



import java.io.IOException;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity {
    private static final String TAG = "CameraActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout previewContainer;
    private SurfaceView surfaceView;
    private ImageButton btnScan;
    private TextView tvResult;
    private String choice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
            return;
        }

        surfaceView = findViewById(R.id.camera_preview);
        previewContainer = findViewById(R.id.preview_container);
        btnScan = findViewById(R.id.btn_scan);
        tvResult = findViewById(R.id.tv_result);


        choice = getIntent().getStringExtra("choice");
        if (choice == null) choice = "fruit";

        mPreview = new CameraPreview(this, new SurfaceView(this));
        // add preview as child of preview_container
        previewContainer.addView(mPreview.getView());



        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tvResult.setText("Scanning...");
                if (mCamera != null) {
                    // take picture
                    mCamera.takePicture(null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            // analyze image
                            String result = analyzeImage(data);
                            tvResult.setText(result);
                            // Restart preview after capture
                            try {
                                mCamera.startPreview();
                            } catch (Exception e) {
                                Log.e(TAG, "startPreview failed", e);
                            }
                        }
                    });
                } else {
                    tvResult.setText("Camera belum siap");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
        // Force SurfaceView square: set height = width
        surfaceView.post(new Runnable() {
            @Override
            public void run() {
                int w = previewContainer.getWidth();
                // make square (4:4)
                surfaceView.getLayoutParams().width = w;
                surfaceView.getLayoutParams().height = w;
                surfaceView.requestLayout();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void openCamera() {
        try {
            mCamera = Camera.open();
            // set default orientation and parameters
            Camera.Parameters params = mCamera.getParameters();

            // choose preview size close to square if possible
            Camera.Size best = null;
            for (Camera.Size sz : params.getSupportedPreviewSizes()) {
                if (best == null) best = sz;
                // choose size with width ~ height to be more square-ish
                if (Math.abs(sz.width - sz.height) < Math.abs(best.width - best.height)) {
                    best = sz;
                }
            }
            if (best != null) {
                params.setPreviewSize(best.width, best.height);
            }

            mCamera.setParameters(params);

            mPreview.setCamera(mCamera);
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat membuka kamera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "openCamera", e);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
            } catch (Exception ignored) {}
            mCamera.release();
            mCamera = null;
        }
    }

    // Very simple image analysis heuristic
    private String analyzeImage(byte[] jpegData) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // downsample
            Bitmap bmp = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
            if (bmp == null) return "Gagal memproses gambar";

            // Crop center square region for analysis
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int size = Math.min(w, h) / 3; // sample center small region
            int cx = w / 2;
            int cy = h / 2;
            int left = Math.max(0, cx - size / 2);
            int top = Math.max(0, cy - size / 2);
            Bitmap sample = Bitmap.createBitmap(bmp, left, top, Math.min(size, w - left), Math.min(size, h - top));

            long sumR = 0, sumG = 0, sumB = 0;
            int count = 0;
            int step = Math.max(1, sample.getWidth() / 50); // sample pixels
            for (int x = 0; x < sample.getWidth(); x += step) {
                for (int y = 0; y < sample.getHeight(); y += step) {
                    int px = sample.getPixel(x, y);
                    int r = (px >> 16) & 0xff;
                    int g = (px >> 8) & 0xff;
                    int b = px & 0xff;
                    sumR += r;
                    sumG += g;
                    sumB += b;
                    count++;
                }
            }

            double avgR = sumR / (double) count;
            double avgG = sumG / (double) count;
            double avgB = sumB / (double) count;

            // heuristic:
            // if green significantly higher than red/blue => FRESH
            // else if red+green high but blue low (brownish) => ROTTEN
            double greenScore = avgG - (avgR + avgB) / 2.0;
            double brownScore = (avgR + avgG) / 2.0 - avgB;

            String label;
            if (greenScore > 18) {
                label = "FRESH";
            } else if (brownScore > 20) {
                label = "ROTTEN";
            } else {
                // fallback: compare brightness and red dominance
                double brightness = (avgR + avgG + avgB) / 3.0;
                if (brightness > 90 && avgG > avgR && avgG > avgB) label = "FRESH";
                else if (avgR > avgG && avgR > avgB) label = "ROTTEN";
                else label = "UNKNOWN";
            }

            // include debug info
            String debug = String.format("%s\navgR=%.1f avgG=%.1f avgB=%.1f", label, avgR, avgG, avgB);
            return debug;

        } catch (Exception e) {
            Log.e(TAG, "analyzeImage", e);
            return "Error saat menganalisis gambar";
        }

    }

    // Inner class for preview
    private static class CameraPreview implements SurfaceHolder.Callback {
        private SurfaceView mSurfaceView;
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private Activity mActivity;

        CameraPreview(Activity activity, SurfaceView sv) {
            mActivity = activity;
            mSurfaceView = sv;
            mHolder = sv.getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        void setCamera(Camera camera) {
            mCamera = camera;
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.setDisplayOrientation(90); // portrait preview
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mHolder.getSurface() == null) return;
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                }
            } catch (Exception ignored) {}

            if (mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Camera released in Activity
        }

        public SurfaceView getView() {
            return mSurfaceView;
        }
    }
}
