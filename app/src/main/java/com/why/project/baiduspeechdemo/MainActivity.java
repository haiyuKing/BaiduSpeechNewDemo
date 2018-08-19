package com.why.project.baiduspeechdemo;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.why.project.baiduspeechdialog.dialog.SpeechBottomSheetDialog;
import com.why.project.baiduspeechdialog.dialog.SpeechLongBottomSheetDialog;

import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = MainActivity.class.getSimpleName();

	private Button mOpenSpeechDialogBtn;
	private Button mOpenSpeechLongDialogBtn;
	private TextView mResultTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		onePermission();

		initViews();
		initEvents();
	}

	private void initViews() {
		mOpenSpeechDialogBtn = findViewById(R.id.btn_openSpeechDialog);
		mOpenSpeechLongDialogBtn = findViewById(R.id.btn_openSpeechLongDialog);
		mResultTv = findViewById(R.id.tv_result);
	}

	private void initEvents() {
		mOpenSpeechDialogBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//打开百度语音对话框
				SpeechBottomSheetDialog speechBottomSheetDialog = SpeechBottomSheetDialog.getInstance(MainActivity.this);
				speechBottomSheetDialog.seOnResultListItemClickListener(new SpeechBottomSheetDialog.OnResultListItemClickListener() {
					@Override
					public void onItemClick(String title) {
						//填充到输入框中
						mResultTv.setText(title);
					}
				});
				speechBottomSheetDialog.show(getSupportFragmentManager(), TAG);
			}
		});
		mOpenSpeechLongDialogBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//打开百度语音对话框
				SpeechLongBottomSheetDialog speechLongBottomSheetDialog = SpeechLongBottomSheetDialog.getInstance(MainActivity.this);
				speechLongBottomSheetDialog.seOnResultListItemClickListener(new SpeechLongBottomSheetDialog.OnResultListItemClickListener() {
					@Override
					public void onItemClick(String title) {
						//填充到输入框中
						mResultTv.setText(mResultTv.getText()+title);
					}
				});
				speechLongBottomSheetDialog.show(getSupportFragmentManager(), TAG);
			}
		});

	}

	/**只有一个运行时权限申请的情况*/
	private void onePermission(){
		RxPermissions rxPermissions = new RxPermissions(MainActivity.this); // where this is an Activity instance
		rxPermissions.request(Manifest.permission.RECORD_AUDIO,
				Manifest.permission.READ_PHONE_STATE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE) //权限名称，多个权限之间逗号分隔开
				.subscribe(new Consumer<Boolean>() {
					@Override
					public void accept(Boolean granted) throws Exception {
						Log.e(TAG, "{accept}granted=" + granted);//执行顺序——1【多个权限的情况，只有所有的权限均允许的情况下granted==true】
						if (granted) { // 在android 6.0之前会默认返回true
							// 已经获取权限
						} else {
							// 未获取权限
							Toast.makeText(MainActivity.this, "您没有授权该权限，请在设置中打开授权", Toast.LENGTH_SHORT).show();
						}
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(Throwable throwable) throws Exception {
						Log.e(TAG,"{accept}");//可能是授权异常的情况下的处理
					}
				}, new Action() {
					@Override
					public void run() throws Exception {
						Log.e(TAG,"{run}");//执行顺序——2
					}
				});
	}
}
