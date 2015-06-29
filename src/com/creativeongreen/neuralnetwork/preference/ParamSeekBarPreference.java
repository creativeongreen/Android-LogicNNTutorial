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

package com.creativeongreen.neuralnetwork.preference;

import com.creativeongreen.neuralnetwork.apps.logic.R;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
*
* @author creativeongreen
* 
* User preference implementation
* 
*/
public class ParamSeekBarPreference extends Preference implements
		OnSeekBarChangeListener {

	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
	private static final String MYPREF_NS = "http://com.creativeongreen.neuralnetwork.preference";

	private TextView tv;
	private int mCurrentValue, mMinValue, mMaxValue;
	private int mSeekBarProgressFactor;

	public ParamSeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.activity_param_seekbar_settings);
		mSeekBarProgressFactor = attrs.getAttributeIntValue(MYPREF_NS,
				"sbProgressFactor", 1);
		mMinValue = attrs.getAttributeIntValue(MYPREF_NS, "minValue", 0);
		mMaxValue = attrs.getAttributeIntValue(ANDROID_NS, "max", 100000);
		mCurrentValue = (int) (attrs.getAttributeFloatValue(
				"http://schemas.android.com/apk/res/android", "defaultValue",
				(float) 1.0) * mSeekBarProgressFactor);
	}

	public ParamSeekBarPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);

		SeekBar seekBar = (SeekBar) view.findViewById(R.id.sb_slider);
		seekBar.setMax((mMaxValue - mMinValue) * mSeekBarProgressFactor);
		seekBar.setProgress(mCurrentValue - mMinValue * mSeekBarProgressFactor);
		seekBar.setOnSeekBarChangeListener(this);

		tv = (TextView) view.findViewById(R.id.tv_seekbar_title);
		displaySeekBarTitle();
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		if (restorePersistedValue) {
			mCurrentValue = getPersistedInt(1);
		} else {
			mCurrentValue = (int) (defaultValue);
		}

		persistInt(mCurrentValue);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// value of progress will be within the range between 0 .. (maxValue - minValue)
		// example: minValue = 50; maxValue = 100;
		// progress equal to 25 (0..100-50) means 50% progress,
		// and its actual value will be 75 (25+50)
		mCurrentValue = progress + mMinValue;
		persistInt(mCurrentValue);
		displaySeekBarTitle();
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	private void displaySeekBarTitle() {
		if (mSeekBarProgressFactor == 1)
			tv.setText(super.getTitle().toString() + " "
					+ (int) (1.0 * mCurrentValue / mSeekBarProgressFactor));
		else
			tv.setText(super.getTitle().toString() + " "
					+ (1.0 * mCurrentValue / mSeekBarProgressFactor));
	}

}
