package com.camuniv.android.javatestapp;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;



public class MainActivity extends ActionBarActivity {
	// カメラインスタンス
	private Camera mCam = null;

	// カメラプレビュークラス
	private CameraPreview mCamPreview = null;

	// 画面タッチの2度押し禁止用フラグ
	private boolean mIsTake = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// カメラインスタンスの取得
		try {
			mCam = Camera.open();
		} catch (Exception e) {
			// エラー
			this.finish();
		}

		// FrameLayout に CameraView クラスを設定
		FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
		mCamPreview = new CameraPreview(this, mCam);
		preview.addView(mCamPreview);

		// mCamPreview に タッチイベントを設定
		mCamPreview.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (!mIsTake) {
						// 撮影中の2度押し禁止用フラグ
						mIsTake = true;
						// 画像取得
						mCam.takePicture(null, null, mPicJpgListener);
					}
				}
				return true;
			}
		});
	}


	@Override
	protected void onPause() {
		super.onPause();
		// カメラ破棄インスタンスを解放
		if (mCam != null) {
			mCam.release();
			mCam = null;
		}
	}


	/**
	 * JPEG データ生成完了時のコールバック
	 */
	private Camera.PictureCallback mPicJpgListener = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			if (data == null) {
				return;
			}

			String saveDir = Environment.getExternalStorageDirectory().getPath() + "/test";

			// SD カードフォルダを取得
			File file = new File(saveDir);

			// フォルダ作成
			if (!file.exists()) {
				if (!file.mkdir()) {
					Log.e("Debug", "Make Dir Error");
				}
			}

			// 画像保存パス
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
			String imgPath = saveDir + "/" + sf.format(cal.getTime()) + ".jpg";

			// ファイル保存
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(imgPath, true);
				fos.write(data);
				fos.close();

				// アンドロイドのデータベースへ登録
				// (登録しないとギャラリーなどにすぐに反映されないため)
				registAndroidDB(imgPath);

			} catch (Exception e) {
				Log.e("Debug", e.getMessage());
			}

			fos = null;

			// takePicture するとプレビューが停止するので、再度プレビュースタート
			mCam.startPreview();

			mIsTake = false;
		}
	};

	/**
	 * アンドロイドのデータベースへ画像のパスを登録
	 * @param path 登録するパス
	 */
	private void registAndroidDB(String path) {
		// アンドロイドのデータベースへ登録
		// (登録しないとギャラリーなどにすぐに反映されないため)
		ContentValues values = new ContentValues();
		ContentResolver contentResolver = MainActivity.this.getContentResolver();
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put("_data", path);
		contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
	}
}
