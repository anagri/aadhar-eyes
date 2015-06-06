package co.creativev.aadhaareyes;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;


public class MainActivity extends Activity {
    private static final String TAG = "OCVSample::Activity";
    private BaseLoaderCallback mLoaderCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLoaderCallback = new OpenCVLoader();
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        org.opencv.android.OpenCVLoader.initAsync(org.opencv.android.OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class OpenCVLoader extends BaseLoaderCallback {
        public OpenCVLoader() {
            super(MainActivity.this);
        }

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    CameraScannerFragment fragment = (CameraScannerFragment) getFragmentManager().findFragmentById(R.id.puzzle_fragment);
                    fragment.onManagerConnected();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    }
}
