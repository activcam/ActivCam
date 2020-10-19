/*
 Copyright 2017-2018 Jens Schreck

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package de.activcam;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import static de.activcam.C.max_webview_width;


public class SettingsActivity extends Activity  {

    EditText urlDynDNS = null;
    EditText urlHomeWifi = null;
    EditText ssidHomeWifi = null;
    EditText videoport1 = null;
    EditText videoport2 = null;
    EditText videoport3 = null;
    EditText videoport4 = null;
    EditText controlport = null;
    EditText auth = null;
    EditText auth_md5 = null;
    Switch hideEventEnd = null;
    Switch hideEventStart = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        updateUI();

        urlDynDNS = (EditText) findViewById(R.id.editTextMotionUrlDynDNS);
        urlHomeWifi = (EditText) findViewById(R.id.editTextMotionUrlHomeWifi);
        ssidHomeWifi = (EditText) findViewById(R.id.editTextSSID);
        videoport1 = (EditText) findViewById(R.id.editTextVideo1Port);
        videoport2 = (EditText) findViewById(R.id.editTextVideo2Port);
        videoport3 = (EditText) findViewById(R.id.editTextVideo3Port);
        videoport4 = (EditText) findViewById(R.id.editTextVideo4Port);
        controlport = (EditText) findViewById(R.id.editTextControlPort);
        auth = (EditText) findViewById(R.id.editTextAuthWeb);
        auth_md5 = (EditText) findViewById(R.id.editTextAuthVideo);
        hideEventEnd = (Switch) findViewById(R.id.switchHideEventButtonEnd);
        hideEventStart = (Switch) findViewById(R.id.switchHideEventButtonStart);

        ViewGroup.LayoutParams lp_video1 = videoport1.getLayoutParams();
        lp_video1.width = (int) max_webview_width / 4;
        videoport1.setLayoutParams(lp_video1);
        videoport2.setLayoutParams(lp_video1);
        videoport3.setLayoutParams(lp_video1);
        videoport4.setLayoutParams(lp_video1);


        urlDynDNS.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_UP)
                    C.trusted_cert="";
                    SharedPreferences pref =
                            PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = pref.edit();
                    editor.remove("Certificate");
                    editor.commit();
                return false;
            }

        });

        auth.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN)
                auth.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                return false;
            }

        });

        if (auth.getText().equals("")) auth.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        auth_md5.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN)
                auth_md5.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                return false;
            }

        });

        if (auth_md5.getText().equals("")) auth_md5.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

        Button buttonMoreCams = (Button) findViewById(R.id.buttonMoreCams);
        buttonMoreCams.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                C.moreCams = !C.moreCams;
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean(C.MORE_CAMS, C.moreCams);
                editor.commit();
                updateUI();

                if (C.moreCams) {
                    Command command = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextAuthVideo)
                    );
                    command.execute(
                            C.server + ":" + C.control + "/"+0+"/config/get?query=stream_authentication",
                            null,
                            C.auth_web
                    );
                    Command command0 = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextVideo1Port)
                    );
                    command0.execute(
                            C.server + ":" + C.control + "/"+0+"/config/get?query=stream_port",
                            null,
                            C.auth_web
                    );
                    Command command1 = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextVideo1Port)
                    );
                    command1.execute(
                            C.server + ":" + C.control + "/"+1+"/config/get?query=stream_port",
                            null,
                            C.auth_web
                    );
                    Command command2 = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextVideo2Port)
                    );
                    command2.execute(
                            C.server + ":" + C.control + "/"+2+"/config/get?query=stream_port",
                            null,
                            C.auth_web
                    );
                    Command command3 = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextVideo3Port)
                    );
                    command3.execute(
                            C.server + ":" + C.control + "/"+3+"/config/get?query=stream_port",
                            null,
                            C.auth_web
                    );
                    Command command4 = new Command(
                            SettingsActivity.this,
                            (EditText) findViewById(R.id.editTextVideo4Port)
                    );
                    command4.execute(
                            C.server + ":" + C.control + "/"+4+"/config/get?query=stream_port",
                            null,
                            C.auth_web
                    );
                }

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        urlDynDNS.setText(C.urlDynDNS);
        urlHomeWifi.setText(C.urlHomeWifi);
        ssidHomeWifi.setText(C.ssidHomeWifi);
        videoport1.setText(C.video11);
        videoport2.setText(C.video21);
        videoport3.setText(C.video12);
        videoport4.setText(C.video22);
        controlport.setText(C.control);
        auth.setText(C.auth_web);
        auth_md5.setText(C.auth_video);
        hideEventEnd.setChecked(C.hide_eventend);
        hideEventStart.setChecked(C.hide_eventstart);
        updateUI();
    }

    @Override
    protected void onPause() {

        C.urlDynDNS = urlDynDNS.getText().toString();
        C.urlHomeWifi = urlHomeWifi.getText().toString();
        C.ssidHomeWifi = ssidHomeWifi.getText().toString();
        C.video11 =videoport1.getText().toString();
        C.video21 =videoport2.getText().toString();
        C.video12 =videoport3.getText().toString();
        C.video22 =videoport4.getText().toString();
        C.control =controlport.getText().toString();
        C.auth_web =auth.getText().toString();
        C.auth_video =auth_md5.getText().toString();
        C.hide_eventend = hideEventEnd.isChecked();
        C.hide_eventstart = hideEventStart.isChecked();
        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("Server", C.urlDynDNS);
        editor.putString("ServerHomeWifi", C.urlHomeWifi);
        editor.putString("SSIDHomeWifi", C.ssidHomeWifi);
        editor.putString("Video1", C.video11);
        editor.putString("Video2", C.video21);
        editor.putString("Video3", C.video12);
        editor.putString("Video4", C.video22);
        editor.putString("Control", C.control);
        editor.putString("Auth", C.auth_web);
        editor.putString("Auth_video", C.auth_video);
        editor.putBoolean("Hide_eventend", C.hide_eventend);
        editor.putBoolean("Hide_eventstart", C.hide_eventstart);
        editor.commit();
        // possibly different version after configuration,
        // new parsing required
        C.version="";

        super.onPause();
    }

    void updateUI(){
        if (C.moreCams) {
            ((EditText) findViewById(R.id.editTextVideo2Port)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textViewVideo2Port)).setVisibility(View.VISIBLE);
            ((EditText) findViewById(R.id.editTextVideo3Port)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textViewVideo3Port)).setVisibility(View.VISIBLE);
            ((EditText) findViewById(R.id.editTextVideo4Port)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textViewVideo4Port)).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textViewVideo1Port)).setText("Cam:1 port/uri");
            ((EditText) findViewById(R.id.editTextVideo1Port)).setHint("8081/1/stream");
            ((Button) findViewById(R.id.buttonMoreCams)).setText(R.string.one_cam);
        } else {
            ((EditText) findViewById(R.id.editTextVideo2Port)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.textViewVideo2Port)).setVisibility(View.GONE);
            ((EditText) findViewById(R.id.editTextVideo3Port)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.textViewVideo3Port)).setVisibility(View.GONE);
            ((EditText) findViewById(R.id.editTextVideo4Port)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.textViewVideo4Port)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.textViewVideo1Port)).setText("Cam:0 port/uri");
            ((EditText) findViewById(R.id.editTextVideo1Port)).setHint("8081/0/stream");
            ((Button) findViewById(R.id.buttonMoreCams)).setText(R.string.more_cams);
        }
    }
}

