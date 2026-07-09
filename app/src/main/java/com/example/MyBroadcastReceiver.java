package com.example;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.research.AXS_COMMAND".equals(intent.getAction())) {
            String targetButton = intent.getStringExtra("target_button");
            if (targetButton == null) {
                // Fallback to checking the older 'key' extra just in case
                targetButton = intent.getStringExtra("key");
            }

            Log.d(TAG, "Manifest Receiver received AXS_COMMAND with target_button: " + targetButton);

            MyAccessibilityService service = MyAccessibilityService.getInstance();
            if (service != null) {
                if (targetButton != null && !targetButton.isEmpty()) {
                    service.performDynamicClick(targetButton);
                } else {
                    CommandLogManager.INSTANCE.addLog("empty_command", "Received AXS_COMMAND but 'target_button' extra was empty/missing.");
                }
            } else {
                Log.w(TAG, "MyAccessibilityService is not currently running or enabled.");
                String displayKey = targetButton != null ? targetButton : "null";
                CommandLogManager.INSTANCE.addLog(displayKey, "Failed: Accessibility Service is not running or enabled.");
            }
        }
    }
}
