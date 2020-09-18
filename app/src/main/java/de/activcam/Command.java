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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import com.albroco.barebonesdigest.DigestAuthentication;
import com.albroco.barebonesdigest.DigestChallengeResponse;

import static android.content.ContentValues.TAG;


public class Command extends AsyncTask<String, String, String> {

    private SharedPreferences pref;
    private TextView txtStatus;
    private TextView txtTime;
    private ListView listConfig;
    private Context context;
    private String version = "";
    private boolean ignore_error = false;

    private void trustEveryone() {
        try {
                HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }});
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, new X509TrustManager[]{new X509TrustManager(){
                    public void checkClientTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {}
                    public void checkServerTrusted(X509Certificate[] chain,
                                                   String authType) throws CertificateException {}
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }}}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(
                        context.getSocketFactory());
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    private void trustDefault() {
        try {
            HostnameVerifier hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
            HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        } catch (Exception e) { // should never happen
            e.printStackTrace();
        }
    }

    public Command(Context context, TextView  txtstatus, TextView  txttime){
        this.txtStatus = txtstatus;
        this.txtTime = txttime;
        this.listConfig = null;
        this.context = context;
        if (txtStatus!=null)txtStatus.setTextColor(Color.parseColor("#404040"));
    }

    public Command(Context context, TextView  txtstatus){
        this.txtStatus = txtstatus;
        this.txtTime = null;
        this.listConfig = null;
        this.context = context;
        this.ignore_error = true;
        if (txtStatus!=null)txtStatus.setTextColor(Color.parseColor("#404040"));
    }

    public Command(Context context, ListView  listconfig){
        this.txtStatus = null;
        this.txtTime = null;
        this.listConfig = listconfig;
        this.context = context;
    }

    @Override
    protected void onProgressUpdate(String... Progress) {
        if (txtStatus!=null) txtStatus.setText(Progress[1]);
        if (listConfig!=null) {
            String[] s = Progress[1].split("\n");
            ArrayAdapter<String> ad;
            ad = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, s);
            listConfig.setAdapter(ad);
        }
        if (txtTime!=null) txtTime.setText(Progress[0]); else this.cancel(true);
     }

    @Override
    protected String doInBackground(String... Params) {

        if (C.trust_just_now) trustEveryone(); else trustDefault();

        URL url;
        HttpURLConnection urlConnection = null;
        String first_response = "";
        String second_response = "";

        try {
            first_response = readURL(Params[0],Params[2]);
            if (!isCancelled() && Params[0].contains("get?query")) {
                first_response = (first_response.split("="))[1];
                first_response = first_response.replace("Done","");
                first_response = first_response.replaceAll(" ","");
                first_response = first_response.replaceAll("\n","");
                publishProgress(null,first_response);
            }
            if (!isCancelled() && version.equals("")) {
                //ToDO get more information about motion installation (camera count, ports, ...)
                try {
                version = readURL(C.server + ":" + C.control+"/",C.auth_web);
                version = version.split("<p class=\"header-right\">Motion ")[1];
                version = version.split("</p")[0];
                version = "Motion "+version;
                } catch (Exception e) {
                    version = "";
                }
            }

            while (!isCancelled() && Params[1]!=null){

                try {

                    second_response = readURL(Params[1],Params[2]);
                    second_response = second_response.replaceAll("<.*?>","");

                } catch (Exception e) {
                    return e.getMessage();
                }

                Date dNow = new Date( );
                SimpleDateFormat ft =
                        new SimpleDateFormat ("hh:mm:ss");
                publishProgress(ft.format(dNow) + " "+ version + " -",second_response);

                try {
                    Thread.sleep(1500);
                } catch(InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

            }

        } catch (Exception e) {
            return e.getMessage();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String error_response) {
        if (txtStatus!=null && !error_response.equals("") && !ignore_error) {
            txtStatus.setText(error_response);
            txtStatus.setTextColor(Color.RED);
        }
        if (listConfig!=null) {
            ArrayAdapter<String> ad;
            String[] er = error_response.split("\n");
            // for (String s: er) s.replaceAll("\\<.*?\\>", "");
            ad = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, error_response.split("\n"));
            listConfig.setAdapter(ad);
        }
    }


    protected String readURL(String webPage, String authString) throws IOException {

        System.out.println("auth string: " + authString);
        byte[] authEncBytes = Base64.encode(authString.getBytes(),Base64.DEFAULT);
        String authStringEnc = new String(authEncBytes);
        Log.d(TAG,"Base64 encoded auth string: " + authStringEnc);
        URL serverUrl = new URL(webPage);

        HttpURLConnection connection = (HttpURLConnection) serverUrl.openConnection();

        String auth_meth = connection.getHeaderField("WWW-Authenticate");

        if (auth_meth==null){
            connection.disconnect();
            connection = (HttpURLConnection) serverUrl.openConnection();
        } else {
            if (auth_meth.startsWith("Basic ")) {
                connection = (HttpURLConnection) serverUrl.openConnection();
                connection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            }
            if (auth_meth.startsWith("Digest ")) {
                String[] s = C.auth_web.split(":");
                // https://github.com/al-broco/bare-bones-digest
                connection = (HttpURLConnection) serverUrl.openConnection();
                DigestAuthentication auth = DigestAuthentication.fromResponse(connection);
                // ...with correct credentials
                auth.username(s[0]).password(s[1]);
                if (!auth.canRespond()) {
                    // No digest challenge or a challenge of an unsupported type - do something else or fail
                    throw new IOException("HTTP: digest challenge");
                }
                connection = (HttpURLConnection) serverUrl.openConnection();
                String rp = auth.getAuthorizationForRequest("GET", connection.getURL().getPath());
                connection.setRequestProperty(DigestChallengeResponse.HTTP_HEADER_AUTHORIZATION, rp);
            }
        }

        int responseCode = connection.getResponseCode();
        if(responseCode != HttpURLConnection.HTTP_OK){
            throw new IOException("HTTP: "+responseCode);
        };

        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        int numCharsRead;
        char[] charArray = new char[1024*1024];
        StringBuffer sb = new StringBuffer();
        while ((numCharsRead = isr.read(charArray)) > 0) {
            sb.append(charArray, 0, numCharsRead);
        }
        return sb.toString();
    }

}

