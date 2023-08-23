package com.itant.rt.ui.follow.action;

import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.util.ScreenUtils;
import com.itant.rt.ui.control.MessageSender;
import com.miekir.mvp.common.log.L;


public class ActionListener implements View.OnTouchListener {
    private int startX;
    private int startY;
    private int endX;
    private int endY;

    private boolean longPressPerformed;

    private float screenWidth = ScreenUtils.getScreenWidth();
    private float screenHeight = ScreenUtils.getScreenHeight();

    private long startMillis;

    private static final int SWIPE_THRESHOLD = ScreenUtils.getScreenWidth()/10;

    public ActionListener(View screenView) {
        screenView.post(new Runnable() {
            @Override
            public void run() {
                screenWidth = screenView.getWidth() * 1.0f;
                screenHeight = screenView.getHeight() * 1.0f;
            }
        });
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        L.e("触摸位置float: " + event.getX() + ", " + event.getY() + ", action" + event.getAction());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            longPressPerformed = false;
            startMillis = System.currentTimeMillis();

            startX = (int) event.getX();
            startY = (int) event.getY();
            endX = startX;
            endY = startY;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            endX = (int) event.getX();
            endY = (int) event.getY();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            // 已经触发了长按
            if (longPressPerformed) {
                return false;
            }

            endX = (int) event.getX();
            endY = (int) event.getY();

            L.e("滑动(" + startX + "," + startY + ") -> (" + endX + ", " + endY + ")");
            MessageSender.INSTANCE.sendCmd(ActionType.SWIPE, startX/screenWidth, startY/screenHeight, endX/screenWidth, endY/screenHeight, System.currentTimeMillis()-startMillis);
        }
        // 为了防止出现只有ACTION_DOWN和ACTION_MOVE，没有ACTION_UP的问题
        return false;
    }

    /**
     * 没有必要，因为onTouch已经包括了点击事件
     */
    public void performClick() {
        if (Math.abs(endX - startX) < SWIPE_THRESHOLD && Math.abs(endY - startY) < SWIPE_THRESHOLD) {
            MessageSender.INSTANCE.sendCmd(ActionType.CLICK, startX/screenWidth, startY/screenHeight, endX/screenWidth, endY/screenHeight, System.currentTimeMillis()-startMillis);
        }
    }

    /**
     * 长按
     */
    public void performLongClick(View svRemote) {
        if (Math.abs(endX - startX) < SWIPE_THRESHOLD && Math.abs(endY - startY) < SWIPE_THRESHOLD) {
            // 长按震动反馈
            svRemote.setHapticFeedbackEnabled(true);
            svRemote.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            svRemote.setHapticFeedbackEnabled(false);

            longPressPerformed = true;
            MessageSender.INSTANCE.sendCmd(ActionType.LONG_CLICK, startX/screenWidth, startY/screenHeight, endX/screenWidth, endY/screenHeight, System.currentTimeMillis()-startMillis);
        }
    }
}