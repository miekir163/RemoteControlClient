package com.itant.rt.ui.follow.action;

import androidx.annotation.Keep;

@Keep
public final class ActionType {
    private ActionType() {
    }

    /**
     * 主控端离开
     */
    public static final int CONTROL_LEAVE = -1;

    /**
     * 点击
     */
    public static final int CLICK = 1;

    /**
     * 长按
     */
    public static final int LONG_CLICK = 2;

    /**
     * 滑动
     */
    public static final int SWIPE = 3;

    /**
     * 回桌面
     */
    public static final int DESKTOP = 4;

    /**
     * 返回
     */
    public static final int BACK = 5;

    /**
     * 多任务
     */
    public static final int OPTIONS = 6;

    /**
     * 设置画面清晰度
     */
    public static final int VIDEO_QUALITY = 7;

    /**
     * 视频尺寸
     */
    public static final int VIDEO_SIZE = 8;
}
