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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;


public class ConfigListActivity extends Activity  {

    Command command = null;
    int camera = 0;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        ListView listView = (ListView) findViewById(R.id.configList);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                String msg = (String) parent.getAdapter().getItem(position);
                input(msg);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = this.getIntent().getIntExtra("camera",0);
        if (command != null) command.cancel(true);
        command = new Command(
                ConfigListActivity.this,
                (ListView) findViewById(R.id.configList)
        );
        command.execute(
                C.server + ":" + C.control + "/"+camera+"/detection/status",
                C.server+":"+C.control+"/"+camera+"/config/list",
                C.auth_web
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    private void input(String txt) {

        input( txt, 0);

    }

    private void input(String txt, int icon) {

        try {

            final String[] m_Text = txt.replaceAll(" ","").split("=");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(m_Text[0]);
            if (icon != 0) builder.setIcon(this.getApplicationContext().getResources().getDrawable(icon));

            // Set up the input
            final EditText input = new EditText(this);
            // Specify the type of input expected;
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setGravity(Gravity.CENTER_HORIZONTAL| Gravity.CENTER_VERTICAL);
            input.setText(m_Text[1]);
            input.setSelection(m_Text[1].length());
            builder.setView(input);
            // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String new_val = input.getText().toString();
                    if (command != null) command.cancel(true);
                    command = new Command(
                            ConfigListActivity.this,
                            (ListView) findViewById(R.id.configList)
                    );
                    command.execute(
                            C.server + ":" + C.control + "/"+camera+"/config/set?"+ m_Text[0] +"="+new_val,
                            C.server + ":" + C.control + "/"+camera+"/config/list",
                            C.auth_web
                    );
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();

        } catch (IndexOutOfBoundsException e) {

        }

    }

}

