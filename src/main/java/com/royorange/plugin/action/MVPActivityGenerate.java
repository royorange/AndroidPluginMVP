package com.royorange.plugin.action;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.royorange.plugin.ui.GenerateMVPActivity;

public class MVPActivityGenerate extends AnAction {
    GenerateMVPActivity dialog = new GenerateMVPActivity();

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        final Project project = e.getData(CommonDataKeys.PROJECT);
//        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        VirtualFile file = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        if(file.isDirectory()){
            final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
            if(view == null){
                return;
            }
            dialog.pack();
            dialog.showDialog(project,file,view.getOrChooseDirectory());
        }
    }
}
