package com.royorange.plugin.model;

import com.intellij.psi.PsiDirectory;

public class GenerateParams {
    //the app package name
    private String appPackageName;
    //the base package name
    private String basePackageName;
    //DI package name
    private String diPackageName;
    //DI module package name
    private String activityBindingModuleClassName;
    //the directory to generate file
    private PsiDirectory directory;
    //the package name of generated class
    private String generateClassPackageName;
    //generate class name
    private String className;
    //generate layout root type
    private String generateLayoutType;
    //the specified presenter of the component
    private String specifiedPresenter;
    //if create layout
    private boolean isCreateLayout = true;
    private boolean isUseDataBinding = true;
    private boolean isUsePresenter = true;
    //the module for di
    private String moduleName;

    public String getBasePackageName() {
        return basePackageName;
    }

    public void setBasePackageName(String basePackageName) {
        this.basePackageName = basePackageName;
    }

    public String getDiPackageName() {
        return diPackageName;
    }

    public void setDiPackageName(String diPackageName) {
        this.diPackageName = diPackageName;
    }

    public boolean isCreateLayout() {
        return isCreateLayout;
    }

    public void setCreateLayout(boolean createLayout) {
        isCreateLayout = createLayout;
    }

    public String getGenerateClassPackageName() {
        return generateClassPackageName;
    }

    public void setGenerateClassPackageName(String generateClassPackageName) {
        this.generateClassPackageName = generateClassPackageName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public PsiDirectory getDirectory() {
        return directory;
    }

    public void setDirectory(PsiDirectory directory) {
        this.directory = directory;
    }

    public boolean isUseDataBinding() {
        return isUseDataBinding;
    }

    public void setUseDataBinding(boolean useDataBinding) {
        isUseDataBinding = useDataBinding;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }

    public boolean isUsePresenter() {
        return isUsePresenter;
    }

    public void setUsePresenter(boolean usePresenter) {
        isUsePresenter = usePresenter;
    }

    public String getGenerateLayoutType() {
        return generateLayoutType;
    }

    public void setGenerateLayoutType(String generateLayoutType) {
        this.generateLayoutType = generateLayoutType;
    }

    public String getSpecifiedPresenter() {
        return specifiedPresenter;
    }

    public void setSpecifiedPresenter(String specifiedPresenter) {
        this.specifiedPresenter = specifiedPresenter;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getActivityBindingModuleClassName() {
        return activityBindingModuleClassName;
    }

    public void setActivityBindingModuleClassName(String activityBindingModuleClassName) {
        this.activityBindingModuleClassName = activityBindingModuleClassName;
    }
}
