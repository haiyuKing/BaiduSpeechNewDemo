package com.why.project.baiduspeechdialog.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.StatusRecogListener;
import com.baidu.aip.asrwakeup3.core.util.MyLogger;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.speech.utils.LogUtil;
import com.why.project.baiduspeechdialog.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by HaiyuKing
 * Used 语音识别底部对话框
 */

public class SpeechBottomSheetDialog extends DialogFragment {
	private static final String TAG = SpeechBottomSheetDialog.class.getSimpleName();

	private Context mContext;
	/**View实例*/
	private View myView;

	private ImageView img_close;
	private ProgressBar loadProgressBar;
	private TextView tv_tishi;
	private RecyclerView result_list;
	private Button btn_start;

	private ArrayList<String> resultWordList;
	private SpeechResultAdapter speechResultAdapter;

	private String BtnStartText = "按一下开始听音";
	private String BtnStopText = "按一下结束听音";
	private String BtnSearchingText = "正在识别";

	private String TishiNoText = "没听清，请重说一遍";

	/**识别控制器，使用MyRecognizer控制识别的流程*/
	protected MyRecognizer myRecognizer;
	/**控制UI按钮的状态*/
	protected int status;

	protected Handler handler;


	public static SpeechBottomSheetDialog getInstance(Context mContext)
	{
		SpeechBottomSheetDialog speechBottomSheetDialog = new SpeechBottomSheetDialog();
		speechBottomSheetDialog.mContext = mContext;

		return speechBottomSheetDialog;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));//设置背景为透明，并且没有标题
		myView = inflater.inflate(R.layout.dialog_bottomsheet_speech, container, false);
		return myView;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);

		initHandler();//初始化handler
		initRecog();//初始化语音

		initViews();
		initDatas();
		initEvents();
	}

	/**
	 * 设置宽度和高度值，以及打开的动画效果
	 */
	@Override
	public void onStart() {
		super.onStart();
		//设置对话框的宽高，必须在onStart中
		DisplayMetrics metrics = new DisplayMetrics();
		this.getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		Window window = this.getDialog().getWindow();
		window.setLayout(metrics.widthPixels, this.getDialog().getWindow().getAttributes().height);
		window.setGravity(Gravity.BOTTOM);//设置在底部
		//打开的动画效果
		//设置dialog的 进出 动画
		getDialog().getWindow().setWindowAnimations(R.style.speechbottomsheetdialog_animation);
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		LogUtil.w(TAG,"{onDismiss}");
		//当对话框消失的时候统一执行销毁语音功能
		destroyRecog();//销毁语音
	}


	private void initViews() {
		img_close = (ImageView) myView.findViewById(R.id.img_close);
		loadProgressBar = (ProgressBar) myView.findViewById(R.id.loadProgressBar);
		tv_tishi = (TextView) myView.findViewById(R.id.tv_tishi);
		result_list = (RecyclerView) myView.findViewById(R.id.result_list);
		btn_start = (Button) myView.findViewById(R.id.btn_start);
	}

	/**初始化数据*/
	private void initDatas() {
		resultWordList = new ArrayList<String>();
		speechResultAdapter = null;
		//设置布局管理器
		LinearLayoutManager linerLayoutManager = new LinearLayoutManager(getActivity());
		result_list.setLayoutManager(linerLayoutManager);

		//可以设置为打开后自动识别语音
		startRecog();
		showProgress();
	}

	private void initEvents() {
		//关闭图标的点击事件
		img_close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		//按钮的点击事件
		btn_start.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (status) {
					case IStatus.STATUS_NONE: // 初始状态
						startRecog();
						status = IStatus.STATUS_WAITING_READY;
						updateBtnTextByStatus();//更改按钮的文本
						//显示加载区域
						showProgress();
						break;
					case IStatus.STATUS_WAITING_READY: // 调用本类的start方法后，即输入START事件后，等待引擎准备完毕。
					case IStatus.STATUS_READY: // 引擎准备完毕。
					case IStatus.STATUS_SPEAKING:
					case IStatus.STATUS_FINISHED: // 长语音情况
					case IStatus.STATUS_RECOGNITION:
						stopRecog();
						status = IStatus.STATUS_STOPPED; // 引擎识别中
						updateBtnTextByStatus();//更改按钮的文本
						break;
					case IStatus.STATUS_STOPPED: // 引擎识别中
						cancelRecog();
						status = IStatus.STATUS_NONE; // 识别结束，回到初始状态
						updateBtnTextByStatus();//更改按钮的文本
						break;
					default:
						break;
				}
			}
		});
	}


	/**
	 * 显示加载进度区域，隐藏其他区域*/
	private void showProgress(){
		loadProgressBar.setVisibility(View.VISIBLE);
		tv_tishi.setVisibility(View.GONE);
		result_list.setVisibility(View.GONE);
	}

	/**
	 * 显示文本提示区域，隐藏其他区域*/
	private void showTishi(){
		tv_tishi.setVisibility(View.VISIBLE);
		loadProgressBar.setVisibility(View.GONE);
		result_list.setVisibility(View.GONE);
	}

	/**
	 * 显示语音结果区域，隐藏其他区域*/
	private void showListView(){
		result_list.setVisibility(View.VISIBLE);
		loadProgressBar.setVisibility(View.GONE);
		tv_tishi.setVisibility(View.GONE);
	}

	//======================================语音相关代码==========================================
	/**
	 * 初始化handler*/
	private void initHandler(){
		handler = new Handler() {
			/*@param msg*/
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				handleMsg(msg);
			}
		};
		MyLogger.setHandler(handler);
	}


	/**
	 * 在onCreate中调用。初始化识别控制类MyRecognizer
	 */
	protected void initRecog() {

		StatusRecogListener listener = new MessageStatusRecogListener(handler);
		myRecognizer = new MyRecognizer(mContext,listener);

		status = IStatus.STATUS_NONE;//默认什么也没有做
	}

	/**
	 * 销毁时需要释放识别资源。
	 */
	protected void destroyRecog() {
		myRecognizer.release();
		Log.i(TAG, "destroyRecog");
	}

	/**
	 * 开始录音，点击“开始”按钮后调用。
	 */
	protected void startRecog() {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put(SpeechConstant.ACCEPT_AUDIO_DATA, false);//是否保存音频
		params.put(SpeechConstant.DISABLE_PUNCTUATION, false);//是否禁用标点符号，在选择输入法模型的前提下生效【不禁用的话，说完一段话，就自带标点符号】
		params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);//暂时不知道什么意思
		params.put(SpeechConstant.PID, 1536); // 普通话 search搜索模型，默认，适用于短句，无逗号，可以有语义
		//params.put(SpeechConstant.VAD_ENDPOINT_TIMEOUT, 0); // 长语音，建议搭配input输入法模型
		myRecognizer.start(params);
	}

	/**
	 * 开始录音后，手动停止录音。SDK会识别在此过程中的录音。点击“停止”按钮后调用。
	 */
	private void stopRecog() {
		myRecognizer.stop();
	}

	/**
	 * 开始录音后，取消这次录音。SDK会取消本次识别，回到原始状态。点击“取消”按钮后调用。
	 */
	private void cancelRecog() {
		myRecognizer.cancel();
	}


	protected void handleMsg(Message msg) {
		Log.e(TAG,"msg.what="+msg.what);
		Log.e(TAG,"msg.obj.toString()="+msg.obj.toString());
		Log.e(TAG,"msg.arg2="+msg.arg2);
		switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
			case IStatus.STATUS_FINISHED:
				//识别结束时候的调用【判断显示结果列表区域还是提示区域】
				if (msg.arg2 == 1) {
					//解析json字符串
					try {
						JSONObject msgObj = new JSONObject(msg.obj.toString());
						String error = msgObj.getString("error");
						if(error.equals("0")){
							//解析结果集合，展现列表
							JSONObject origin_resultObj = msgObj.getJSONObject("origin_result");
							JSONObject resultObj = origin_resultObj.getJSONObject("result");
							JSONArray wordList = resultObj.getJSONArray("word");

							initList(wordList);//初始化集合数据

							showListView();
						}else if(error.equals("7")){
							tv_tishi.setText(TishiNoText);
							showTishi();
						}else{//应该根据不同的状态值，显示不同的提示
							tv_tishi.setText(TishiNoText);
							showTishi();
						}
					} catch (JSONException e) {
						e.printStackTrace();
						tv_tishi.setText(TishiNoText);
						showTishi();
					}
				}else if(msg.arg2 == 0){//无网络的情况
					//解析json字符串{"origin_result":{"sn":"","error":2,"desc":"Network is not available","sub_error":2100},"error":2,"desc":"Network is not available","sub_error":2100}
					try {
						JSONObject msgObj = new JSONObject(msg.obj.toString());
						JSONObject origin_resultObj = msgObj.getJSONObject("origin_result");
						String error = origin_resultObj.getString("error");
						if(error.equals("2")){
							//解析结果集合，展现列表
							String desc = origin_resultObj.getString("desc");
							Toast.makeText(mContext,desc,Toast.LENGTH_SHORT).show();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				status = msg.what;
				updateBtnTextByStatus();
				break;
			case IStatus.STATUS_NONE:
			case IStatus.STATUS_READY:
			case IStatus.STATUS_SPEAKING:
			case IStatus.STATUS_RECOGNITION:
				status = msg.what;
				updateBtnTextByStatus();
				break;
			default:
				break;
		}
	}

	/**更改按钮的文本*/
	private void updateBtnTextByStatus() {
		switch (status) {
			case IStatus.STATUS_NONE:
				btn_start.setText(BtnStartText);
				btn_start.setEnabled(true);
				break;
			case IStatus.STATUS_WAITING_READY:
			case IStatus.STATUS_READY:
			case IStatus.STATUS_SPEAKING:
			case IStatus.STATUS_RECOGNITION:
				btn_start.setText(BtnStopText);
				btn_start.setEnabled(true);
				break;
			case IStatus.STATUS_STOPPED:
				btn_start.setText(BtnSearchingText);
				btn_start.setEnabled(true);
				break;
			default:
				break;
		}
	}

	//========================================更改列表==========================
	/**获取集合数据，并显示*/
	private void initList(JSONArray wordList){
		//先清空
		if(resultWordList.size() > 0){
			resultWordList.clear();
		}
		//再赋值
		for(int i=0;i<wordList.length();i++){
			String wordItem = "";
			try {
				wordItem = wordList.getString(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			resultWordList.add(wordItem);
		}

		if(speechResultAdapter == null){
			//设置适配器
			speechResultAdapter = new SpeechResultAdapter(getActivity(), resultWordList);
			result_list.setAdapter(speechResultAdapter);
			//添加分割线
			//设置添加删除动画

			//调用ListView的setSelected(!ListView.isSelected())方法，这样就能及时刷新布局
			result_list.setSelected(true);
		}else{
			speechResultAdapter.notifyDataSetChanged();
		}

		speechResultAdapter.setOnItemClickLitener(new SpeechResultAdapter.OnItemClickLitener() {
			@Override
			public void onItemClick(int position) {
				dismiss();
				if(mOnResultListItemClickListener != null){
					mOnResultListItemClickListener.onItemClick(resultWordList.get(position));
				}
			}
		});
	}

	//=========================语音列表项的点击事件监听==============================
	public static abstract interface OnResultListItemClickListener
	{
		//语音结果列表项的点击事件接口
		public abstract void onItemClick(String title);
	}

	private OnResultListItemClickListener mOnResultListItemClickListener;

	public void seOnResultListItemClickListener(OnResultListItemClickListener mOnResultListItemClickListener)
	{
		this.mOnResultListItemClickListener = mOnResultListItemClickListener;
	}

}
