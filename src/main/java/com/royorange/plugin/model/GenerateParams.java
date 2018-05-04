package com.royorange.plugin.model;

import com.intellij.psi.PsiDirectory;

public class GenerateParams {
    private String appPackageName;
    private String basePackageName;
    private String diPackageName;
    private PsiDirectory directory;
    private String generateClassPackageName;
    private String className;
    private boolean isCreateLayout;
    private boolean isUseDataBinding;
    private boolean isUsePresenter;

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
}
