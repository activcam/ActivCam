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

public class C {

    static String id ="";
    static String server ="";
    static String urlDynDNS ="";
    static String urlHomeWifi ="";
    static String ssidHomeWifi ="";
    static String control ="";
    static String auth_web ="";
    static String auth_video ="";
    static int my_req_write = 0;
    static boolean trust_just_now = false;
    static String trusted_cert = "";
    static String version = "";

    static boolean consent_fcm = false;

    static int max_webview_height = 0;
    static int max_webview_width = 0;

    // false = only one camera
    static boolean moreCams = false;
    static boolean hide_eventend = true;
    static boolean hide_eventstart = true;
    static final String MORE_CAMS = "premium";

    static String video11 ="";
    static String video21 ="";
    static String video12 ="";
    static String video22 ="";

}
