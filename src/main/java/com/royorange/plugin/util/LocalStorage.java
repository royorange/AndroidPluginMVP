package com.royorange.plugin.util;

import com.intellij.ide.util.PropertiesComponent;

public class LocalStorage {

    public static void saveBasePackage(String packageName){
        PropertiesComponent.getInstance().setValue("basePackageName",packageName);
    }

    public static String loadBasePackage(){
        return PropertiesComponent.getInstance().getValue("basePackageName","com.arvato.sephora.app.presentation.view.base");
    }

    public static void saveLayoutType(String type){
        PropertiesComponent.getInstance().setValue("layoutType",type);
    }

    public static String loadLayoutType(){
        return PropertiesComponent.getInstance().getValue("layoutType","com.arvato.sephora.app.presentation.view.base");
    }

    public static void saveDIPackage(String packageName){
        PropertiesComponent.getInstance().setValue("diPackageName",packageName);
    }

    public static String loadDIPackage(){
        return PropertiesComponent.getInstance().getValue("diPackageName","com.arvato.sephora.app.presentation.internal.di");
    }

    public static String loadHolderModule(){
        return PropertiesComponent.getInstance().getValue("holderModule","");
    }

    public static void saveHolderModule(String className){
        PropertiesComponent.getInstance().setValue("holderModule",className);
    }

    public static String loadActivityBindingModule(){
        return PropertiesComponent.getInstance().getValue("activityBindingModule","com.arvato.sephora.app.presentation.internal.di.modules.ActivityBindingModule");
    }

    public static void saveActivityBindingModule(String className){
        PropertiesComponent.getInstance().setValue("activityBindingModule",className);
    }

    public static void saveCreateLayout(boolean b){
        PropertiesComponent.getInstance().setValue("createLayout",b);
    }

    public static boolean loadCreateLayout(){
        return PropertiesComponent.getInstance().getBoolean("createLayout",true);
    }

    public static void saveUseDatabinding(boolean b){
        PropertiesComponent.getInstance().setValue("useDataBinding",b);
    }

    public static boolean loadUseDatabinding(){
        return PropertiesComponent.getInstance().getBoolean("useDataBinding",true);
    }

    public static void saveUsePresenter(boolean b){
        PropertiesComponent.getInstance().setValue("usePresenter",b);
    }

    public static boolean loadUsePresenter(){
        return PropertiesComponent.getInstance().getBoolean("usePresenter",true);
    }
}
