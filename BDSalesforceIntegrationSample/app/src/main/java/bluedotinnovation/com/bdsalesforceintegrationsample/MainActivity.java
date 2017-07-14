package bluedotinnovation.com.bdsalesforceintegrationsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.WindowManager;
import android.widget.TextView;


/*
 * @author Bluedot Innovation
 * Copyright (c) 2016 Bluedot Innovation. All rights reserved.
 * MainApplication demonstrates the implementation Bluedot Point SDK and related callbacks.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TEXT_LOG_BROADCAST = "bdsalesforceintegrationsample.logtextbroadcast";
    private TextView textViewLog;
    LogReceiver logReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        textViewLog = (TextView) findViewById(R.id.textViewLog);
        textViewLog.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onStart() {
        super.onStart();
        logReceiver = new LogReceiver();
        registerReceiver(logReceiver, new IntentFilter(TEXT_LOG_BROADCAST));
    }

    public final class LogReceiver extends BroadcastReceiver {

        public LogReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String logInfo = intent.getStringExtra("logInfo");
            textViewLog.append(logInfo + "\n");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(logReceiver);
    }
}
