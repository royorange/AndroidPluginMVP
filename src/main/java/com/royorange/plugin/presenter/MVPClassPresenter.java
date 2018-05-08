package com.royorange.plugin.presenter;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.royorange.plugin.model.GenerateParams;

public interface MVPClassPresenter {

    void generate(Project project, PsiFile file);

    void generateActivity(Project project, GenerateParams params);

    void generateFragment(Project project, GenerateParams params);
}
