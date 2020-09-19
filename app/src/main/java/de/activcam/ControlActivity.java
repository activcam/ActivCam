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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;
import java.util.Random;

import static android.content.ContentValues.TAG;
import static de.activcam.C.max_webview_height;
import static de.activcam.C.max_webview_width;

public class ControlActivity extends Activity {

    private class SSLTolerentWebViewClient extends WebViewClient {

        final String[] s = C.auth_video.split(":");
        Context cc = null;

        public SSLTolerentWebViewClient(Context cc){
            this.cc = cc;
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView webview, HttpAuthHandler handler, String host, String realm) {
            if (s.length==2)
                handler.proceed(s[0], s[1]);
        }

        // credits to @Heath Borders at http://stackoverflow.com/questions/20228800/how-do-i-validate-an-android-net-http-sslcertificate-with-an-x509trustmanager
        private Certificate getX509Certificate(SslCertificate sslCertificate){
            Bundle bundle = SslCertificate.saveState(sslCertificate);
            byte[] bytes = bundle.getByteArray("x509-certificate");
            if (bytes == null) {
                return null;
            } else {
                try {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    return certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                } catch (CertificateException e) {
                    return null;
                }
            }
        }

        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                // Get cert from SslError
                SslCertificate sslCertificate = error.getCertificate();
                final Certificate cert = getX509Certificate(sslCertificate);
                if (C.trusted_cert.equals(cert.getPublicKey().toString())){
                    C.trust_just_now = true;
                    // Do do my action here
                    if (command != null) command.cancel(true);
                    command = new Command(
                            ControlActivity.this,
                            (TextView) findViewById(R.id.videoStatus),
                            (TextView) findViewById(R.id.cameraTime)
                    );
                    command.execute(
                            C.server + ":" + C.control + "/"+camera+"/detection/status",
                            C.server + ":" + C.control + "/"+camera+"/detection/status",
                            C.auth_web
                    );
                    handler.proceed();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(cc);
                builder.setTitle(getString(R.string.do_you_trust));
                builder.setMessage(error.getCertificate() +
                        "\n\n" + cert.getPublicKey().toString());
                builder.setIcon(android.R.drawable.ic_partial_secure);
                builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        C.trust_just_now = true;
                        C.trusted_cert = cert.getPublicKey().toString();
                        SharedPreferences pref =
                                PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("Certificate", C.trusted_cert);
                        editor.commit();

                        // Do do my action here
                        if (command != null) command.cancel(true);
                        command = new Command(
                                ControlActivity.this,
                                (TextView) findViewById(R.id.videoStatus),
                                (TextView) findViewById(R.id.cameraTime)
                        );
                        command.execute(
                                C.server + ":" + C.control + "/"+camera+"/detection/status",
                                C.server + ":" + C.control + "/"+camera+"/detection/status",
                                C.auth_web
                        );
                        handler.proceed();
                    }
                });
                builder.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        C.trust_just_now = false;
                        // Do do my action here
                        if (command != null) command.cancel(true);
                        command = new Command(
                                ControlActivity.this,
                                (TextView) findViewById(R.id.videoStatus),
                                (TextView) findViewById(R.id.cameraTime)
                        );
                        command.execute(
                                C.server + ":" + C.control + "/"+camera+"/detection/status",
                                C.server + ":" + C.control + "/"+camera+"/detection/status",
                                C.auth_web
                        );
                        handler.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                if (view.equals(webView11)) dialog.show();
        };

    }

    Command command = null;
    int camera = 0;
    long h2 = 0;

    float x, y, x1, y1;

    WebView webView11;
    WebView webView21;
    WebView webView12;
    WebView webView22;

    LinearLayout motionpanel;
    LinearLayout camerapanel;
    LinearLayout camera_row1;
    LinearLayout camera_row2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        C.id = pref.getString("ID","");
        if (C.id.equals("")) {
            byte[] bposid = new byte[6];
            new Random().nextBytes(bposid);
            C.id = Base64.encodeToString(bposid,
                    Base64.URL_SAFE+Base64.NO_PADDING+Base64.NO_WRAP)
                            .toUpperCase().replace("_","");
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("ID", C.id);
            editor.commit();
        }

        C.urlDynDNS = pref.getString("Server", "");
        C.urlHomeWifi = pref.getString("ServerHomeWifi", "");
        C.ssidHomeWifi = pref.getString("SSIDHomeWifi", "");

        C.control = pref.getString("Control", "8080");
        C.auth_web = pref.getString("Auth", "");
        C.auth_video = pref.getString("Auth_video", "");
        C.video11 = pref.getString("Video1", "");
        C.video21 = pref.getString("Video2", "");
        C.video12 = pref.getString("Video3", "");
        C.video22 = pref.getString("Video4", "");
        C.moreCams = pref.getBoolean(C.MORE_CAMS, false);
        C.consent_fcm = pref.getBoolean("CONSENT_FCM", false);
        C.hide_eventend = pref.getBoolean("Hide_eventend", true);
        C.hide_eventstart = pref.getBoolean("Hide_eventstart", true);
        C.trusted_cert = pref.getString("Certificate", "");

        webView11 = (WebView) findViewById(R.id.webView11);
        webView21 = (WebView) findViewById(R.id.webView21);
        webView12 = (WebView) findViewById(R.id.webView12);
        webView22 = (WebView) findViewById(R.id.webView22);

        motionpanel = (LinearLayout) findViewById(R.id.motionpanel);
        camerapanel = (LinearLayout) findViewById(R.id.camerapanel);
        camera_row1 = (LinearLayout) findViewById(R.id.camera_row1);
        camera_row2 = (LinearLayout) findViewById(R.id.camera_row2);

        ImageButton buttonStart = (ImageButton) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ControlActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(getString(R.string.start_detection));
                builder.setIcon(android.R.drawable.ic_media_play);
                builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        // Do do my action here
                        if (command != null) command.cancel(true);
                        command = new Command(
                                ControlActivity.this,
                                (TextView) findViewById(R.id.videoStatus),
                                (TextView) findViewById(R.id.cameraTime)
                        );
                        command.execute(
                                C.server + ":" + C.control + "/"+camera+"/detection/start",
                                C.server + ":" + C.control + "/"+camera+"/detection/status",
                                C.auth_web
                        );
                        dialog.dismiss();
                    }

                });

                builder.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // I do not need any action here you might
                        dialog.dismiss();
                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        ImageButton buttonSnapshot = (ImageButton) findViewById(R.id.buttonSnapshot);
        buttonSnapshot.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    snapshot();
                }
            }
        );

        ImageButton buttonStop = (ImageButton) findViewById(R.id.buttonPause);
        buttonStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (command != null) command.cancel(true);
                command = new Command(
                        ControlActivity.this,
                        (TextView) findViewById(R.id.videoStatus),
                        (TextView) findViewById(R.id.cameraTime)
                );
                command.execute(
                        C.server + ":" + C.control + "/"+camera+"/detection/pause",
                        C.server + ":" + C.control + "/"+camera+"/detection/status",
                        C.auth_web
                );
                toast(getString(R.string.stop_detection));
            }
        });

        ImageButton buttonEventEnd = (ImageButton) findViewById(R.id.buttonEventEnd);
        buttonEventEnd.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (command != null) command.cancel(true);
                command = new Command(
                        ControlActivity.this,
                        (TextView) findViewById(R.id.videoStatus),
                        (TextView) findViewById(R.id.cameraTime)
                );
                command.execute(
                        C.server + ":" + C.control + "/" + camera + "/action/eventend",
                        C.server + ":" + C.control + "/" + camera + "/detection/status",
                        C.auth_web
                );
                toast(getString(R.string.event_end));
            }
        });

        ImageButton buttonEventStart = (ImageButton) findViewById(R.id.buttonEventStart);
        buttonEventStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (command != null) command.cancel(true);
                command = new Command(
                        ControlActivity.this,
                        (TextView) findViewById(R.id.videoStatus),
                        (TextView) findViewById(R.id.cameraTime)
                );
                command.execute(
                        C.server + ":" + C.control + "/" + camera + "/action/eventstart",
                        C.server + ":" + C.control + "/" + camera + "/action/makemovie",
                        C.auth_web
                );
                toast(getString(R.string.make_movie));
            }
        });

         final TextView txtViewStatus = (TextView) findViewById(R.id.videoStatus);

         TextView txtAktCamera = (TextView) findViewById(R.id.txtMotion);
         txtAktCamera.setOnClickListener(new View.OnClickListener() {

             @Override
             public void onClick(View v) {
                    messagingDialog();
             }
         });

        ImageButton buttonRefresh = (ImageButton) findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video11);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        Button buttonCam0 = (Button) findViewById(R.id.buttonCAM0);
        buttonCam0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    camera=0; onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video11);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        Button buttonCam1 = (Button) findViewById(R.id.buttonCAM1);
        buttonCam1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    camera=1; onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video11);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        Button buttonCam2 = (Button) findViewById(R.id.buttonCAM2);
        buttonCam2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    camera=2; onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video21);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        Button buttonCam3 = (Button) findViewById(R.id.buttonCAM3);
        buttonCam3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    camera=3; onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video12);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        Button buttonCam4 = (Button) findViewById(R.id.buttonCAM4);
        buttonCam4.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    camera=4; onResume();
                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                    loadUrl(webView11,C.server + ":" + C.video22);
                } catch (ActivityNotFoundException e) {
                    txtViewStatus.setText(e.getMessage());
                }
            }
        });

        txtViewStatus.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent pwdintent = new Intent(ControlActivity.this, SettingsActivity.class);
                startActivity(pwdintent);
            }
        });

        findViewById(R.id.buttonSettings).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent pwdintent = new Intent(ControlActivity.this, SettingsActivity.class);
                startActivity(pwdintent);
            }
        });

        findViewById(R.id.layoutCamera).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent pwdintent = new Intent(ControlActivity.this, ConfigListActivity.class);
                pwdintent.putExtra("camera",camera);
                startActivity(pwdintent);
            }
        });

        findViewById(R.id.txtLicense).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                textinfo(getString(R.string.license), getString(R.string.text_license));
            }
        });

        // Snapshot
        findViewById(R.id.webView11).setOnTouchListener(new View.OnTouchListener() {
             @Override
             public boolean onTouch(View v, MotionEvent motionEvent) {
                 switch (motionEvent.getAction()) {
                     case MotionEvent.ACTION_DOWN:
                         x = motionEvent.getX();
                         y = motionEvent.getY();
                         break;
                     case MotionEvent.ACTION_UP:
                         x1 = motionEvent.getX();
                         y1 = motionEvent.getY();
                         if ((y1 == y) || (x1 == x)) {
                             snapshot();
                         } else {
                             if (camera==0 && C.moreCams)
                                 try {
                                     camera=1; onResume();
                                     ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                     // loadUrl(webView11,C.server + ":" + C.video11);
                                 } catch (ActivityNotFoundException e) {
                                     txtViewStatus.setText(e.getMessage());
                                 }
                             else if (camera!=0 && C.moreCams)
                                 try {
                                     camera=0; onResume();
                                     ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                     // loadUrl(webView11,C.server + ":" + C.video11);
                                 } catch (ActivityNotFoundException e) {
                                     txtViewStatus.setText(e.getMessage());
                                 }
                         }
                         break;
                     default:
                 }
                 return true;
             }

         });
        findViewById(R.id.webView21).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        x1 = motionEvent.getX();
                        y1 = motionEvent.getY();
                        if ((y1 == y) || (x1 == x)) {
                            snapshot();
                        } else {
                            if (camera==0 && C.moreCams)
                                try {
                                    camera=2; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video21);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                            else if (camera!=0 && C.moreCams)
                                try {
                                    camera=0; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video11);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                        }
                        break;
                    default:
                }
                return true;
            }

        });
        // Snapshot
        findViewById(R.id.webView12).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        x1 = motionEvent.getX();
                        y1 = motionEvent.getY();
                        if ((y1 == y) || (x1 == x)) {
                            snapshot();
                        } else {
                            if (camera==0 && C.moreCams)
                                try {
                                    camera=3; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video12);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                            else if (camera!=0 && C.moreCams)
                                try {
                                    camera=0; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video11);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                        }
                        break;
                    default:
                }
                return true;
            }

        });
        // Snapshot
        findViewById(R.id.webView22).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        x1 = motionEvent.getX();
                        y1 = motionEvent.getY();
                        if ((y1 == y) || (x1 == x)) {
                            snapshot();
                        } else {
                            if (camera==0 && C.moreCams)
                                try {
                                    camera=4; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video22);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                            else if (camera!=0 && C.moreCams)
                                try {
                                    camera=0; onResume();
                                    ((TextView) findViewById(R.id.txtAktCamera)).setText("CAM:"+camera);
                                    loadUrl(webView11,C.server + ":" + C.video11);
                                } catch (ActivityNotFoundException e) {
                                    txtViewStatus.setText(e.getMessage());
                                }
                        }
                        break;
                    default:
                }
                return true;
            }

        });


        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        if (size.x > size.y) {
            max_webview_height =size.y;
            max_webview_width =size.x;
        } else {
            max_webview_height = size.x;
            max_webview_width =size.y;
        }

        subscription();
    }

    @Override
    public void onPause() {
        if (command!=null) command.cancel(true);
        command=null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        C.server = C.urlDynDNS;
        String ssid = getSSID();
        if (!C.urlHomeWifi.equals(""))
            if (C.ssidHomeWifi.equals(ssid) || C.ssidHomeWifi.equals(""))
                C.server = C.urlHomeWifi;

        if (C.hide_eventend){
            findViewById(R.id.buttonEventEnd).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonEventEnd).setVisibility(View.VISIBLE);
        };
        if (C.hide_eventstart){
            findViewById(R.id.buttonEventStart).setVisibility(View.GONE);
        } else {
            findViewById(R.id.buttonEventStart).setVisibility(View.VISIBLE);
        };

        if (C.moreCams) {
            ((Button) findViewById(R.id.buttonCAM0)).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.buttonCAM1)).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.buttonCAM0)).setEnabled(camera==0 ? false : true);
            ((Button) findViewById(R.id.buttonCAM1)).setEnabled(camera==1 ? false : true);
            ((Button) findViewById(R.id.buttonCAM2)).setEnabled(camera==2 ? false : true);
            ((Button) findViewById(R.id.buttonCAM3)).setEnabled(camera==3 ? false : true);
            ((Button) findViewById(R.id.buttonCAM4)).setEnabled(camera==4 ? false : true);
            if (C.video11.isEmpty())
                ((Button) findViewById(R.id.buttonCAM1)).setVisibility(View.GONE);
            else
                ((Button) findViewById(R.id.buttonCAM1)).setVisibility(View.VISIBLE);
            if (C.video21.isEmpty())
                ((Button) findViewById(R.id.buttonCAM2)).setVisibility(View.GONE);
            else
                ((Button) findViewById(R.id.buttonCAM2)).setVisibility(View.VISIBLE);
            if (C.video12.isEmpty())
                ((Button) findViewById(R.id.buttonCAM3)).setVisibility(View.GONE);
            else
                ((Button) findViewById(R.id.buttonCAM3)).setVisibility(View.VISIBLE);
            if (C.video22.isEmpty())
                ((Button) findViewById(R.id.buttonCAM4)).setVisibility(View.GONE);
            else
                ((Button) findViewById(R.id.buttonCAM4)).setVisibility(View.VISIBLE);
        } else {
            ((Button) findViewById(R.id.buttonCAM0)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.buttonCAM1)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.buttonCAM2)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.buttonCAM3)).setVisibility(View.GONE);
            ((Button) findViewById(R.id.buttonCAM4)).setVisibility(View.GONE);
        }


        try {
            Configuration config = getResources().getConfiguration();
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                portrait();
            }
            if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                landscape();
            }

            if (C.server.isEmpty()) {
                webView11.loadUrl("file:///android_asset/web_hi_res_512.png");
                (findViewById(R.id.cameraConfig)).setVisibility(View.GONE);
            }
            else {
                (findViewById(R.id.cameraConfig)).setVisibility(View.VISIBLE);
                if (camera==0) {
                    if (!C.video11.isEmpty()) loadUrl (webView11,C.server + ":" + C.video11);
                    if (!C.video21.isEmpty()) loadUrl (webView21,C.server + ":" + C.video21);
                    if (!C.video12.isEmpty()) loadUrl (webView12,C.server + ":" + C.video12);
                    if (!C.video22.isEmpty()) loadUrl (webView22,C.server + ":" + C.video22);
                }

                if (command != null) command.cancel(true);
                command = new Command(
                        ControlActivity.this,
                        (TextView) findViewById(R.id.videoStatus),
                        (TextView) findViewById(R.id.cameraTime)
                );
                command.execute(
                        C.server + ":" + C.control + "/"+0+"/detection/status",
                        C.server + ":" + C.control + "/"+camera+"/detection/status",
                        C.auth_web
                );
            }

            } catch (Exception e) {
                Log.d("WebView", e.getMessage());
            }

        webView11.getSettings().setLoadWithOverviewMode(true);
        webView21.getSettings().setLoadWithOverviewMode(true);
        webView12.getSettings().setLoadWithOverviewMode(true);
        webView22.getSettings().setLoadWithOverviewMode(true);

        webView11.getSettings().setUseWideViewPort(true);
        webView21.getSettings().setUseWideViewPort(true);
        webView12.getSettings().setUseWideViewPort(true);
        webView22.getSettings().setUseWideViewPort(true);

    }


    void snapshot(){
        if (command != null) command.cancel(true);
        command = new Command(
                ControlActivity.this,
                (TextView) findViewById(R.id.videoStatus),
                (TextView) findViewById(R.id.cameraTime)
        );
        command.execute(
                C.server + ":" + C.control + "/" + camera + "/action/snapshot",
                C.server + ":" + C.control + "/" + camera + "/detection/status",
                C.auth_web
        );

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    C.my_req_write);
        } else {

            WebView webview = findViewById(R.id.webView11);
            webview.setDrawingCacheEnabled(true);
            Bitmap b = Bitmap.createBitmap(webview.getDrawingCache());
            webview.setDrawingCacheEnabled(false);
            String result =
                    MediaStore.Images.Media.insertImage(getContentResolver(),b,"","");
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }

    }

    private void landscape() {

        ((LinearLayout) findViewById(R.id.motionpanel)).setVisibility(View.GONE);
        findViewById(R.id.buttonRefresh).setVisibility(View.GONE);

        if (!C.moreCams || camera>0) {

            webView11.setVisibility(View.VISIBLE);
            webView21.setVisibility(View.GONE);
            webView12.setVisibility(View.GONE);
            webView22.setVisibility(View.GONE);

            camera_row2.setVisibility(View.GONE);

        } else {

            webView11.setVisibility(C.video11.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            webView21.setVisibility(C.video21.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            webView12.setVisibility(C.video12.isEmpty() ? View.INVISIBLE : View.VISIBLE);
            webView22.setVisibility(C.video22.isEmpty() ? View.INVISIBLE : View.VISIBLE);

            camera_row2.setVisibility(C.video21.isEmpty() && C.video22.isEmpty() ? View.GONE : View.VISIBLE);
        }

    }

    private void portrait() {

        findViewById(R.id.motionpanel).setVisibility(View.VISIBLE);
        if (C.moreCams)
            findViewById(R.id.buttonRefresh).setVisibility(View.GONE);
        else
            findViewById(R.id.buttonRefresh).setVisibility(View.VISIBLE);

        if (!C.moreCams || camera>0) {

            webView11.setVisibility(View.VISIBLE);
            webView21.setVisibility(View.GONE);
            webView12.setVisibility(View.GONE);
            webView22.setVisibility(View.GONE);

            camera_row2.setVisibility(View.GONE);

        } else {

            webView11.setVisibility(C.video11.isEmpty() ? View.GONE : View.VISIBLE);
            webView21.setVisibility(C.video21.isEmpty() ? View.GONE : View.VISIBLE);
            webView12.setVisibility(C.video12.isEmpty() ? View.GONE : View.VISIBLE);
            webView22.setVisibility(C.video22.isEmpty() ? View.GONE : View.VISIBLE);

            camera_row2.setVisibility(C.video21.isEmpty() && C.video22.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void loadUrl(WebView webview, String url){
        final String[] s = C.auth_video.split(":");
        webview.setWebViewClient(new SSLTolerentWebViewClient(this));
        webview.loadUrl(url);
    }

    private String getSSID(){
        try {

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService (Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo ();

            String ssid = "";
            List<WifiConfiguration> listOfConfigurations = wifiManager.getConfiguredNetworks();

            for (int index = 0; index < listOfConfigurations.size(); index++) {
                WifiConfiguration configuration = listOfConfigurations.get(index);
                if (configuration.networkId == wifiInfo.getNetworkId()) {
                    if (ssid.equals("")) {
                        return configuration.SSID.replace("\"","");
                    }
                }
            }
            return "";

        }catch(Exception e) {
            textinfo(getString(android.R.string.dialog_alert_title),e.getMessage().toString(),
                    android.R.drawable.ic_dialog_alert);
            return "";
        }
    }

    private void textinfo(String title, String txt) {
        textinfo(title, txt, 0);
    }

    private void textinfo(String title, String txt, int icon) {
        AlertDialog.Builder myalert = new AlertDialog.Builder(this);
        myalert.setTitle(title);
        myalert.setMessage(txt);
        if (icon == 0) icon = R.mipmap.ic_launcher;
        myalert.setIcon(this.getApplicationContext().getResources().getDrawable(icon));
        myalert.setNeutralButton(getString(android.R.string.ok), null);
        myalert.setCancelable(true);
        myalert.show();
    }

    private void toast(final String pMessage) {
        Toast.makeText(ControlActivity.this.getApplicationContext(), pMessage, Toast.LENGTH_SHORT).show();
    }

    private void messagingDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.push_notifications));
        String s = "";
        if (C.consent_fcm){
            s = getString(R.string.text_revoke);
            builder.setNegativeButton(getString(R.string.text_consent_revoke), (dialog, which) -> {
                C.consent_fcm = false;
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("CONSENT_FCM");
                editor.apply();
                dialog.cancel();
            });
        }
        else {
            builder.setNegativeButton(getString(R.string.text_consent_cancel), (dialog, which) -> {
                dialog.cancel();
            });
        }

        if (C.consent_fcm){
            builder.setPositiveButton(getString(R.string.text_consent_cancel), (dialog, which) -> {
                dialog.cancel();
            });
        } else {
            s = getString(R.string.text_consent);
            // Set up the buttons
            builder.setPositiveButton(getString(R.string.text_consent_agree), (dialog, which) -> {
                C.consent_fcm = true;
                SharedPreferences pref =
                        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("CONSENT_FCM", C.consent_fcm);
                editor.apply();
                subscription();
                dialog.cancel();
            });
        }


        // Set up the buttons
        builder.setNeutralButton(getString(R.string.text_consent_help), (dialog, which) -> {
            try {
                String l = getString(R.string.url_help_notify);
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(l));
                startActivity(myIntent);
            } catch (ActivityNotFoundException e) {
                toast(e.getMessage());
            }
            dialog.cancel();
        });

        builder.setMessage(s + " " + C.id);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.show();
    }

    private void subscription(){
        if (C.consent_fcm) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create channel to show notifications.
                String channelId  = "fcm_default_channel";
                String channelName = C.id;
                NotificationManager notificationManager =
                        getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                        channelName, NotificationManager.IMPORTANCE_LOW));
            }

            // If a notification message is tapped, any data accompanying the notification
            // message is available in the intent extras. In this sample the launcher
            // intent is fired when the notification is tapped, so any accompanying data would
            // be handled here. If you want a different intent fired, set the click_action
            // field of the notification message to the desired intent. The launcher intent
            // is used when no click_action is specified.
            //
            // Handle possible data accompanying notification message.
            // [START handle_data_extras]
            if (getIntent().getExtras() != null) {
                for (String key : getIntent().getExtras().keySet()) {
                    Object value = getIntent().getExtras().get(key);
                    Log.d(TAG, "Key: " + key + " Value: " + value);
                }
            }
            // [END handle_data_extras]

            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w(TAG, "getInstanceId failed", task.getException());
                                return;
                            }
                            // Get new Instance ID token
                            String token = task.getResult().getToken();
                        }
                    });
            Log.d(TAG, "Subscribing to topic");
            // [START subscribe_topics]
            FirebaseMessaging.getInstance().subscribeToTopic(C.id)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            String msg = "";
                            if (!task.isSuccessful()) {
                                msg = "FCM registration failed";
                                Log.d(TAG, msg);
                                Toast.makeText(ControlActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            // [END subscribe_topics]
        }
    }

}
