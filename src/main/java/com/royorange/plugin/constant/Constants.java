package com.royorange.plugin.constant;

import java.util.HashMap;

public class Constants {
    public static final String[] LAYOUT = {"CoordinatorLayout","ConstraintLayout","FrameLayout","LinearLayout","RelativeLayout"};

    public static final String[] PRESENTER = {"RxPresenter"};

    public static HashMap<String,String> LayoutMap = new HashMap<>();

    static {
        LayoutMap.put("CoordinatorLayout","android.support.design.widget");
        LayoutMap.put("ConstraintLayout","android.support.constraint");
        LayoutMap.put("FrameLayout","");
        LayoutMap.put("LinearLayout","");
        LayoutMap.put("RelativeLayout","");
    }
}
