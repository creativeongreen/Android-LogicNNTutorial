/*
 * Copyright (C) 2015 creativeongreen
 *
 * Licensed either under the Apache License, Version 2.0, or (at your option)
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation (subject to the "Classpath" exception),
 * either version 2, or any later version (collectively, the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     http://www.gnu.org/licenses/
 *     http://www.gnu.org/software/classpath/license.html
 *
 * or as provided in the LICENSE.txt file that accompanied this code.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.creativeongreen.neuralnetwork.apps.logic;

import com.creativeongreen.neuralnetwork.nets.BackpropagationNet;
import com.creativeongreen.neuralnetwork.preference.UserSettingActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
*
* @author creativeongreen
* 
* MainActivity
* 
*/
public class MainActivity extends Activity {

	private static final String LOG_TAG = "NN_MainActivity";

	private static final int INTENT_REQUEST_SETTINGS_UPDATED = 1;

	private static final int MSG_HANDLER_TRAINING_FINISHED = 1;

	private static final int SETTINGS_PARAMS_GENERAL_PERCENT = 100;
	private static final String SETTINGS_PARAMS_KEY_NUM_HIDDEN_NEURONS = "paramNumHiddenNeurons";
	private static final String SETTINGS_PARAMS_KEY_LEARNING_RATE = "paramLearningRate";
	private static final String SETTINGS_PARAMS_KEY_MOMENTUM = "paramMomentum";
	private static final String SETTINGS_PARAMS_KEY_GLOBAL_ERROR = "paramGlobalError";
	private static final String SETTINGS_PARAMS_KEY_MAX_EPOCH = "paramMaxEpoch";
	private static final String SETTINGS_PARAMS_KEY_NOISE_DEGREE = "paramNoiseDegree";

	public static double LOGIC_INPUT[][] = { { 0.0, 0.0 }, { 0.0, 1.0 },
			{ 1.0, 0.0 }, { 1.0, 1.0 } };

	public static double XOR_EXPECTED[][] = { { 0.0 }, { 1.0 }, { 1.0 },
			{ 0.0 } };

	public static double AND_EXPECTED[][] = { { 0.0 }, { 0.0 }, { 0.0 },
			{ 1.0 } };

	public static double OR_EXPECTED[][] = { { 0.0 }, { 1.0 }, { 1.0 }, { 1.0 } };

	public static int numHiddenNeurons = 2;
	public static double dLearningRate = 0.25;
	public static double dMomentum = 0.9;
	public static double dGlobalError = 0.00001;
	public static double dMaxEpoch = 10000;
	public static double dNoiseDegree = 0.2;

	BackpropagationNet net;
	private RelativeLayout rlLogic;
	private ProgressBar pbOnProcessing;
	private TextView tvTestResults;
	private Thread tTraining;
	private long lStartTime;
	private double[][] expectedOutput;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Log.d(LOG_TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		rlLogic = (RelativeLayout) findViewById(R.id.ll_1);
		pbOnProcessing = (ProgressBar) findViewById(R.id.pb_1);
		tvTestResults = (TextView) findViewById(R.id.test_results);
		getSettings();

	}

	@Override
	protected void onPause() {
		// Log.d(LOG_TAG, "onPause()");

		if (tTraining != null) {
			net.stopTraining();
			tTraining.interrupt();
			tTraining = null;
		}

		// place super pause at last
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Log.d(LOG_TAG, "onCreateOptionsMenu()");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, UserSettingActivity.class);
			startActivityForResult(i, INTENT_REQUEST_SETTINGS_UPDATED);
			break;

		case R.id.menu_action_about:
			showAbout();
			break;

		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	public void onClickStartTraining1(View v) {
		startTraining(v, XOR_EXPECTED);
	}

	public void onClickStartTraining2(View v) {
		startTraining(v, AND_EXPECTED);
	}

	public void onClickStartTraining3(View v) {
		startTraining(v, OR_EXPECTED);
	}

	public void onClickTestWithNoise(View v) {
		tvTestResults.append("\nTest with noise:\n");

		double[] noiseInputData = new double[LOGIC_INPUT[0].length];

		// degree range: -1*dNoiseDegree ~ 1*dNoiseDegree
		for (int i = 0; i < LOGIC_INPUT.length; i++) {
			for (int k = 0; k < LOGIC_INPUT[i].length; k++) {
				noiseInputData[k] = LOGIC_INPUT[i][k] + LOGIC_INPUT[i][k]
						* ((dNoiseDegree * 2 * Math.random()) - dNoiseDegree);
			}
			tvTestResults.append(String.format("%1.4f", noiseInputData[0])
					+ "  " + String.format("%1.4f", noiseInputData[1]) + "  ("
					+ expectedOutput[i][0] + ") -> ");

			net.feedForward(noiseInputData);
			double[] y = net.getOutputResults();
			for (int j = 0; j < y.length; j++) {
				tvTestResults.append(String.format("%1.10f", y[j]) + " ");
			}
			tvTestResults.append("\n");
		}
	}

	public void startTraining(View v, final double[][] expected) {
		rlLogic.setVisibility(View.GONE);
		pbOnProcessing.setVisibility(View.VISIBLE);
		showParamsInfo();
		Toast.makeText(this, getString(R.string.start_training),
				Toast.LENGTH_SHORT).show();

		net = new BackpropagationNet(LOGIC_INPUT[0].length, numHiddenNeurons,
				expected[0].length, dLearningRate, dMomentum, dMaxEpoch,
				dGlobalError);

		lStartTime = System.currentTimeMillis();

		tTraining = new Thread(new Runnable() {
			@Override
			public void run() {

				net.train(LOGIC_INPUT, expected);
				// hTrainMessageReceiver.obtainMessage(MSG_HANDLER_TRAINING_FINISHED).sendToTarget();
				Message message = hTrainMessageReceiver.obtainMessage();
				message.what = MSG_HANDLER_TRAINING_FINISHED;
				message.obj = new Object[] { (double[][]) expected };
				message.sendToTarget();

			} // /run()
		});

		tTraining.start();

	}

	private final Handler hTrainMessageReceiver = new Handler() {

		@Override
		public void handleMessage(final Message message) {
			switch (message.what) {
			case MSG_HANDLER_TRAINING_FINISHED:
				toastTrainingFinished();
				long lTimeDifference = System.currentTimeMillis() - lStartTime;

				final Object[] args = (Object[]) message.obj;
				double[][] expected = (double[][]) args[0];
				tvTestResults.append("\nTraining Finished: timelapse= "
						+ lTimeDifference + " millis, epoch= " + net.getEpoch()
						+ "\nTest Results:\n");
				expectedOutput = expected;

				// test
				for (int i = 0; i < LOGIC_INPUT.length; i++) {
					net.feedForward(LOGIC_INPUT[i]);
					tvTestResults.append(LOGIC_INPUT[i][0] + "  "
							+ LOGIC_INPUT[i][1] + "  (" + expected[i][0]
							+ ") -> ");
					double[] y = net.getOutputResults();
					for (int j = 0; j < y.length; j++) {
						tvTestResults.append(String.format("%1.10f", y[j])
								+ " ");
					}
					tvTestResults.append("\n");
				}

				pbOnProcessing.setVisibility(View.GONE);
				rlLogic.setVisibility(View.VISIBLE);
				break;

			default:
				throw new IllegalArgumentException("cannot handle message");
			} // switch
		} // handleMessage(Message)

	};

	private void toastTrainingFinished() {
		Toast.makeText(this, getString(R.string.training_finished),
				Toast.LENGTH_SHORT).show();

	}

	private void getSettings() {
		SharedPreferences settingsPreferences;

		settingsPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		numHiddenNeurons = settingsPreferences.getInt(
				SETTINGS_PARAMS_KEY_NUM_HIDDEN_NEURONS, 2);
		dLearningRate = settingsPreferences.getInt(
				SETTINGS_PARAMS_KEY_LEARNING_RATE, 30)
				* 1.0
				/ SETTINGS_PARAMS_GENERAL_PERCENT;
		dMomentum = settingsPreferences
				.getInt(SETTINGS_PARAMS_KEY_MOMENTUM, 90)
				* 1.0
				/ SETTINGS_PARAMS_GENERAL_PERCENT;
		dGlobalError = Math.pow(
				10,
				(-1)
						* settingsPreferences.getInt(
								SETTINGS_PARAMS_KEY_GLOBAL_ERROR, 5));
		dMaxEpoch = settingsPreferences.getInt(SETTINGS_PARAMS_KEY_MAX_EPOCH,
				10000);// * 1.0 / SETTINGS_PARAMS_MAX_EPOCH;

		dNoiseDegree = settingsPreferences.getInt(
				SETTINGS_PARAMS_KEY_NOISE_DEGREE, 20)
				* 1.0
				/ SETTINGS_PARAMS_GENERAL_PERCENT;

		showParamsInfo();

	}

	private void showParamsInfo() {
		tvTestResults.setText("Number of Hidden Neurons = " + numHiddenNeurons
				+ "\nLearning Rate = " + dLearningRate + "\nMomentum = "
				+ dMomentum + "\nGlobal Error = "
				+ String.format("%2.1e", dGlobalError) + "\nMax. Epoch = "
				+ String.valueOf((int) dMaxEpoch)
				+ "\nInput Data Noise Degree = (+/-) "
				+ String.format("%2.2f", dNoiseDegree * 100) + "%\n");
	}

	private void showAbout() {
		final StringBuilder about_string = new StringBuilder();
		about_string.append(getString(R.string.app_name));
		String version_name = "UNKNOWN_VERSION";
		int version_code = -1;
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version_name = pInfo.versionName;
			version_code = pInfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.w(LOG_TAG, "showAbout/NameNotFoundException: " + e.getMessage());
			e.printStackTrace();
		}
		about_string.append(" v" + version_name + "\n");
		about_string.append(getString(R.string.version_code) + ": "
				+ version_code + "\n");
		about_string.append(getString(R.string.about_desc));

		popNotification(getString(R.string.action_about),
				about_string.toString());
	}

	private void popNotification(String title, String message) {
		// new AlertDialog.Builder(this).setTitle(title).setMessage(message).create().show();
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialogInterface, int i) {
			}
		});

		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.show();
	}

	// Listen for results.
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Log.d(LOG_TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" +
		// resultCode);
		// super.onActivityResult(requestCode, resultCode, data);

		// See which child activity is calling us back.
		switch (requestCode) {
		case INTENT_REQUEST_SETTINGS_UPDATED:
			getSettings();
			break;

		default:
			break;
		}
	}

}
