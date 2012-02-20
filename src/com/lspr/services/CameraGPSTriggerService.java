package com.lspr.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Toast;

import com.lspr.activities.SettingActivity;
import com.lspr.constants.LSPRConstants;
import com.lspr.modules.Mail;

public class CameraGPSTriggerService extends Service {

	private static final String TAG = "Service";
	private SharedPreferences prefs;

	public CameraGPSTriggerService() {
		super();
		prefs = null;
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			// 1 as the parameter is for the front camera, 0 is for back camera
			c = Camera.open(1); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.release(); // release the camera for other applications
			mCamera = null;
		}
	}

	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	private Camera mCamera;

	// picture call back start
	PictureCallback mPicture = new PictureCallback() {

		public void onPictureTaken(byte[] data, Camera camera) {

			// Get app preference
			prefs = getApplicationContext().getSharedPreferences(
					LSPRConstants.PREF_NAME, 0);
			// create unique file name
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());

			final String FILENAME = "IMG_" + timeStamp + ".jpg";

			try {

				FileOutputStream fos = openFileOutput(FILENAME,
						Context.MODE_PRIVATE);
				fos.write(data);
				fos.close();

				// Emails out picture and location
				// ----------------------------------------------------------------------
				// Email module grabs from SD, attach and sends email
				prefs = SettingActivity.getPreferences(getApplicationContext());
				final String emailText = prefs.getString(
						LSPRConstants.PREF_EMAIL, "email@domain.com");
				final String emailPassword = prefs.getString(
						LSPRConstants.PREF_EMAIL_PASS, "email_password");

				new Thread(new Runnable() {
					public void run() {
						sendMail(emailText, emailPassword);
					}

					// add another parameter for this method to get GPS string
					private void sendMail(String email, String pass) {

						final Mail m = new Mail();
						// String usertext =
						// intent.getExtras().getString("email");
						String usertext = email;
						String emailPass = pass;
						String[] toArr = { usertext };
						// m.setPass(intent.getExtras().getString("pass"));
						m.setPass(emailPass);
						m.setUser(usertext);
						m.setTo(toArr);
						m.setFrom(usertext);
						m.setSubject("Capture");
						m.setBody("GPS Coor.\nLat:\nLong:\n");
						try {
							//create exact path to picture
							String path_to_picture = prefs.getString(
									LSPRConstants.PREF_PATH_NAME, "file_path")
									+ File.separator + FILENAME;
							//attach picture
							m.addAttachment(path_to_picture);

							// the following if statement may not be needed
							// outside of
							// testing
							// would replace with just m.send()
							// or just comment out the Toast lines
							if (m.send()) {
								Log.e(TAG, "Email sent successful.");
								// Toast.makeText(this,
								// "Email was sent successfully.",
								// Toast.LENGTH_LONG).show();
							} else {
								Log.e(TAG, "Email was not sent.");
								// Toast.makeText(this, "Email was not sent.",
								// Toast.LENGTH_LONG)
								// .show();
							}
						} catch (Exception e) {
							Log.e(TAG, "Problem sending email.");
							e.printStackTrace();
							// Toast.makeText(
							// this,
							// "There was a problem sending the email.\nBody:"
							// + m.getBody() + "\nSubject: " + m.getSubject()
							// + "\nFrom: " + m.getFrom() + "\nUser: "
							// + m.getUser(),
							// // + "\nPass: " + m.getPass(),
							// Toast.LENGTH_LONG).show();

						}
					}
				}).start();

				stopSelf();

			} catch (FileNotFoundException e) {
				// Log.d(TAG, "File not found: " + e.getMessage());
			} catch (IOException e) {
				// Log.d(TAG, "Error accessing file: " + e.getMessage());
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onCreate");

	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		Log.d(TAG, "onDestroy");
		releaseCamera();
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();

		// Capture picture
		// ----------------------------------------------------------------------

		// Create an instance of Camera
		mCamera = getCameraInstance();
		Log.d(TAG, "onStart");
		SurfaceView view = new SurfaceView(this.getApplicationContext());
		try {
			mCamera.setPreviewDisplay(view.getHolder());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mCamera.startPreview();
		// change ringer to silent to disable shutter sound
		AudioManager am = (AudioManager) this.getApplicationContext()
				.getSystemService(Context.AUDIO_SERVICE);
		am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		// rotate to correct position
		Camera.Parameters params = mCamera.getParameters();
		params.setRotation(90);
		mCamera.setParameters(params);
		// take picture
		mCamera.takePicture(null, null, mPicture);
		// return to normal sound settings

		// Grab GPS location
		// ----------------------------------------------------------------------
		// Toast.makeText(this, "Recorded your GPS location!",
		// Toast.LENGTH_SHORT).show();

	}

}
