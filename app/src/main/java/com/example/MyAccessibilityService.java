package com.example;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class MyAccessibilityService extends AccessibilityService {

    private static final String TAG = "MyAccessibilityService";
    private static MyAccessibilityService sInstance = null;

    private BroadcastReceiver mCommandReceiver = null;

    public static MyAccessibilityService getInstance() {
        return sInstance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "Accessibility Service Connected");
        sInstance = this;
        CommandLogManager.INSTANCE.addLog("service_connected", "Accessibility Service connected and active");

        registerCommandReceiver();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No-op - we perform actions reactively based on incoming broadcasts
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Accessibility Service Interrupted");
        CommandLogManager.INSTANCE.addLog("service_interrupted", "Accessibility Service interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Accessibility Service Destroyed");
        if (sInstance == this) {
            sInstance = null;
        }
        CommandLogManager.INSTANCE.addLog("service_disconnected", "Accessibility Service disconnected");
        unregisterCommandReceiver();
    }

    private void registerCommandReceiver() {
        if (mCommandReceiver != null) return;

        mCommandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.research.AXS_COMMAND".equals(intent.getAction())) {
                    // Extract target_button string extra
                    String targetButton = intent.getStringExtra("target_button");
                    if (targetButton == null) {
                        // Fallback to checking the older 'key' extra just in case
                        targetButton = intent.getStringExtra("key");
                    }
                    
                    Log.d(TAG, "Service Receiver received AXS_COMMAND with command: " + targetButton);
                    if (targetButton != null && !targetButton.isEmpty()) {
                        handleServiceCommand(targetButton);
                    } else {
                        CommandLogManager.INSTANCE.addLog("empty_command", "Received AXS_COMMAND but 'target_button' extra was empty/missing.");
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.research.AXS_COMMAND");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mCommandReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(mCommandReceiver, filter);
        }
    }

    private void unregisterCommandReceiver() {
        if (mCommandReceiver != null) {
            try {
                unregisterReceiver(mCommandReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Failed to unregister dynamic receiver", e);
            }
            mCommandReceiver = null;
        }
    }

    /**
     * Main entry point to handle incoming command actions.
     * If the command matches a known system/global action keyword, it executes that.
     * Otherwise, it dynamically scans the active screen window to click matching text.
     */
    public void handleServiceCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            CommandLogManager.INSTANCE.addLog("empty_command", "Command cannot be empty");
            return;
        }

        String lowerCmd = command.trim().toLowerCase();
        boolean isGlobalAction = false;
        int actionId = -1;
        String actionName = "";

        switch (lowerCmd) {
            case "back":
            case "@back":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_BACK;
                actionName = "Back";
                break;
            case "home":
            case "@home":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_HOME;
                actionName = "Home";
                break;
            case "notifications":
            case "@notifications":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_NOTIFICATIONS;
                actionName = "Notifications";
                break;
            case "recents":
            case "@recents":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_RECENTS;
                actionName = "Recents";
                break;
            case "quick_settings":
            case "@quick_settings":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_QUICK_SETTINGS;
                actionName = "Quick Settings";
                break;
            case "power_dialog":
            case "@power_dialog":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_POWER_DIALOG;
                actionName = "Power Dialog";
                break;
            case "lock_screen":
            case "@lock_screen":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_LOCK_SCREEN;
                actionName = "Lock Screen";
                break;
            case "split_screen":
            case "@split_screen":
                isGlobalAction = true;
                actionId = GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN;
                actionName = "Split Screen";
                break;
        }

        if (isGlobalAction) {
            boolean success = performGlobalAction(actionId);
            if (success) {
                CommandLogManager.INSTANCE.addLog(command, "Success: Performed global action: " + actionName);
            } else {
                CommandLogManager.INSTANCE.addLog(command, "Failed: Could not perform global action: " + actionName);
            }
        } else {
            // Scan screen and perform dynamic click
            performDynamicClick(command);
        }
    }

    /**
     * Scans the active screen window content, locates any element matching targetButton text,
     * and performs a click action.
     */
    public void performDynamicClick(String targetButton) {
        if (targetButton == null || targetButton.trim().isEmpty()) {
            CommandLogManager.INSTANCE.addLog("click_error", "Target button text cannot be empty");
            return;
        }

        Log.d(TAG, "Starting screen scan for button: '" + targetButton + "'");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root in active window is null. Cannot scan screen.");
            CommandLogManager.INSTANCE.addLog(targetButton, "Failed: Active window content is null (screen locked or off)");
            return;
        }

        AccessibilityNodeInfo targetNode = findNodeByText(rootNode, targetButton);
        if (targetNode != null) {
            boolean success = performClickOnNode(targetNode);
            Log.d(TAG, "Found target node! Click performance result: " + success);
            if (success) {
                CommandLogManager.INSTANCE.addLog(targetButton, "Success: Found element and simulated click!");
            } else {
                CommandLogManager.INSTANCE.addLog(targetButton, "Failed: Element found, but click action could not be executed");
            }
            targetNode.recycle();
        } else {
            Log.w(TAG, "Could not find any element matching '" + targetButton + "' on the screen");
            CommandLogManager.INSTANCE.addLog(targetButton, "Failed: Element with matching text/description not found on screen");
        }
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo root, String targetText) {
        if (root == null) return null;

        // Check node's primary text
        CharSequence text = root.getText();
        if (text != null && text.toString().trim().equalsIgnoreCase(targetText.trim())) {
            return root;
        }

        // Check node's content description
        CharSequence contentDesc = root.getContentDescription();
        if (contentDesc != null && contentDesc.toString().trim().equalsIgnoreCase(targetText.trim())) {
            return root;
        }

        // Search children recursively
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo found = findNodeByText(child, targetText);
                if (found != null) {
                    return found; // Return the exact matching node
                }
            }
        }
        return null;
    }

    private boolean performClickOnNode(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // If the node itself is clickable, execute click
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        // If not clickable, search up the parent hierarchy for a clickable parent (e.g. Button wrapping Text)
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            boolean success = performClickOnNode(parent);
            parent.recycle();
            return success;
        }

        return false;
    }
}
