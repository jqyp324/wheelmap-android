/*
 * #%L
 * Wheelmap - App
 * %%
 * Copyright (C) 2011 - 2012 Michal Harakal - Michael Kroez - Sozialhelden e.V.
 * %%
 * Wheelmap App based on the Wheelmap Service by Sozialhelden e.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS-IS" BASIS
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wheelmap.android.activity;

import org.holoeverywhere.app.Activity;
import org.wheelmap.android.fragment.WheelchairStateFragment;
import org.wheelmap.android.fragment.WheelchairStateFragment.OnWheelchairState;
import org.wheelmap.android.model.Extra;
import org.wheelmap.android.model.WheelchairState;
import org.wheelmap.android.online.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import de.akquinet.android.androlog.Log;

@Activity.Addons(Activity.ADDON_SHERLOCK)
public class WheelchairStateActivity extends Activity implements
        OnWheelchairState {

    private final static String TAG = WheelchairStateActivity.class
            .getSimpleName();

    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frame_empty);
        Log.d(TAG, "onCreate");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        FragmentManager fm = getSupportFragmentManager();
        mFragment = (WheelchairStateFragment) fm
                .findFragmentByTag(WheelchairStateFragment.TAG);
        if (mFragment != null) {
            return;
        }

        int stateId = getIntent().getIntExtra(Extra.WHEELCHAIR_STATE,
                Extra.UNKNOWN);
        mFragment = WheelchairStateFragment.newInstance(WheelchairState
                .valueOf(stateId));

        fm.beginTransaction()
                .add(R.id.content, mFragment,
                        WheelchairStateFragment.TAG).commit();
    }

    @Override
    public void onWheelchairStateSelect(WheelchairState state) {
        Intent intent = new Intent();
        intent.putExtra(Extra.WHEELCHAIR_STATE, state.getId());
        setResult(RESULT_OK, intent);
        finish();
    }

}
