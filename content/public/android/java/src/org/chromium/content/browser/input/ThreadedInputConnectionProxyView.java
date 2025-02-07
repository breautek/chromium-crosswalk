// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser.input;

import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a fake View that is only exposed to InputMethodManager.
 */
public class ThreadedInputConnectionProxyView extends View {
    private static final String TAG = "cr_Ime";
    private static final boolean DEBUG_LOGS = false;

    private final Handler mImeThreadHandler;
    private final View mContainerView;
    private final AtomicBoolean mFocused = new AtomicBoolean();
    private final AtomicBoolean mWindowFocused = new AtomicBoolean();
    private final AtomicReference<IBinder> mWindowToken = new AtomicReference<>();
    private final AtomicReference<View> mRootView = new AtomicReference<>();

    ThreadedInputConnectionProxyView(
            Context context, Handler imeThreadHandler, View containerView) {
        super(context);
        mImeThreadHandler = imeThreadHandler;
        mContainerView = containerView;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setVisibility(View.VISIBLE);
        if (DEBUG_LOGS) Log.w(TAG, "constructor");

        mFocused.set(mContainerView.hasFocus());
        mWindowFocused.set(mContainerView.hasWindowFocus());
        mWindowToken.set(mContainerView.getWindowToken());
        mRootView.set(mContainerView.getRootView());
    }

    public void onOriginalViewFocusChanged(boolean gainFocus) {
        mFocused.set(gainFocus);
    }

    public void onOriginalViewWindowFocusChanged(boolean gainFocus) {
        mWindowFocused.set(gainFocus);
    }

    public void onOriginalViewAttachedToWindow() {
        mWindowToken.set(mContainerView.getWindowToken());
        // Note: this is an approximation of the real behavior.
        // Real root view may change upon addView / removeView, but this is good
        // enough for IME purpose.
        mRootView.set(mContainerView.getRootView());
    }

    public void onOriginalViewDetachedFromWindow() {
        mWindowToken.set(null);
        // Note: we are not asking mContainerView.getRootView() here. We cannot get the correct
        // root view here as ViewRootImpl's mParent is set to null *after* this call.
        // In vanilla Android, getRootView() is never called when window is detaching or detached
        // anyways.
        mRootView.set(null);
    }

    @Override
    public Handler getHandler() {
        if (DEBUG_LOGS) Log.w(TAG, "getHandler");
        return mImeThreadHandler;
    }

    @Override
    public boolean checkInputConnectionProxy(View view) {
        if (DEBUG_LOGS) Log.w(TAG, "checkInputConnectionProxy");
        return mContainerView == view;
    }

    @Override
    public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
        if (DEBUG_LOGS) Log.w(TAG, "onCreateInputConnection");
        return ThreadUtils.runOnUiThreadBlockingNoException(new Callable<InputConnection>() {
            @Override
            public InputConnection call() throws Exception {
                return mContainerView.onCreateInputConnection(outAttrs);
            }
        });
    }

    @Override
    public boolean hasWindowFocus() {
        if (DEBUG_LOGS) Log.w(TAG, "hasWindowFocus");
        return mWindowFocused.get();
    }

    @Override
    public View getRootView() {
        if (DEBUG_LOGS) Log.w(TAG, "getRootView");
        return mRootView.get();
    }

    @Override
    public boolean onCheckIsTextEditor() {
        if (DEBUG_LOGS) Log.w(TAG, "onCheckIsTextEditor");
        // We do not allow Android apps to override WebView#onCheckIsTextEditor() for now.
        return true;
    }

    @Override
    public boolean isFocused() {
        if (DEBUG_LOGS) Log.w(TAG, "isFocused");
        return mFocused.get();
    }

    @Override
    public IBinder getWindowToken() {
        if (DEBUG_LOGS) Log.w(TAG, "getWindowToken");
        return mWindowToken.get();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (DEBUG_LOGS) Log.w(TAG, "onWindowFocusChanged:" + hasWindowFocus);
        super.onWindowFocusChanged(hasWindowFocus);
    }
}