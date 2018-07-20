package com.royorange.plugin.ui;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.PackageChooser;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.royorange.plugin.constant.Constants;
import com.royorange.plugin.model.GenerateParams;
import com.royorange.plugin.presenter.MVPClassPresenter;
import com.royorange.plugin.presenter.MVPJavaPresenter;
import com.royorange.plugin.presenter.ResourcePresenter;
import com.royorange.plugin.util.LocalStorage;
import com.royorange.plugin.util.Utils;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class GenerateMVPActivity extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textBasePackage;
    private JButton buttonChooseBasePackage;
    private JTextField textActivityName;
    private JTextField textDIName;
    private JButton buttonChooseDI;
    private JCheckBox checkboxCreateLayout;
    private JCheckBox checkUseDataBinding;
    private JCheckBox checkUsePresenter;
    private JComboBox comboSelectLayout;
    private JComboBox comboPresenter;
    private JTextField textBindingModule;
    private JButton buttonChooseBindingModule;
    private Project project;
    private GenerateParams params;
    private VirtualFile file;
    private PsiDirectory directory;
    private MVPClassPresenter presenter;
    private ResourcePresenter resourcePresenter;
    private boolean isFirstShow = true;
    private TreeClassChooser classChooser = null;



    public GenerateMVPActivity() {
        presenter = new MVPJavaPresenter();
        resourcePresenter = new ResourcePresenter();
        buttonOK.requestFocusInWindow();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        initView();
        initListener();
    }

    public void showDialog(Project project,VirtualFile file, PsiDirectory directory){
        this.file = file;
        this.directory = directory;
        this.project = project;
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final int x = (screenSize.width - getWidth()) / 2;
        final int y = (screenSize.height - getHeight()) / 2;
        setSize(720,320);
        setLocation(x,y);
        isFirstShow = false;
        setVisible(true);
    }

    private void initView(){
        textDIName.setText(LocalStorage.loadDIPackage());
        textBasePackage.setText(LocalStorage.loadBasePackage());
        textBindingModule.setText(LocalStorage.loadActivityBindingModule());
        checkboxCreateLayout.setSelected(LocalStorage.loadCreateLayout());
        checkUseDataBinding.setSelected(LocalStorage.loadUseDatabinding());
        checkUsePresenter.setSelected(LocalStorage.loadUsePresenter());
        setupSelectLayout();
        setupSelectPresenter();
    }

    private void setupSelectPresenter(){
        comboSelectLayout.setEnabled(checkUsePresenter.isSelected());
        List<String> presenter = new ArrayList<>();
        int initIndex = 0;
        for(int i = 0; i < Constants.PRESENTER.length; i++){
            presenter.add(Constants.PRESENTER[i]);
        }
        ComboBoxModel<String> layoutModel = new ListComboBoxModel<String>(presenter);
        comboPresenter.setModel(layoutModel);
        comboPresenter.setSelectedIndex(initIndex);
    }

    private void setupSelectLayout(){
        comboSelectLayout.setEnabled(checkboxCreateLayout.isSelected());
        List<String> layout = new ArrayList<>();
        int initIndex = 0;
        String lastType = LocalStorage.loadLayoutType();
        for(int i = 0; i < Constants.LAYOUT.length; i++){
            if(lastType.equals(Constants.LAYOUT[i])){
                initIndex = i;
            }
            layout.add(Constants.LAYOUT[i]);
        }
        ComboBoxModel<String> layoutModel = new ListComboBoxModel<String>(layout);
        comboSelectLayout.setModel(layoutModel);
        comboSelectLayout.setSelectedIndex(initIndex);
    }

    private void initListener(){
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        buttonChooseBasePackage.addActionListener(e -> {
            String packageName = addPackagePatternFilter(textBasePackage,LocalStorage.loadBasePackage());
            if(packageName!=null){
                LocalStorage.saveBasePackage(packageName);
            }
        });

        buttonChooseDI.addActionListener(e -> {
            String packageName = addPackagePatternFilter(textDIName,LocalStorage.loadDIPackage());
            if(packageName!=null){
                LocalStorage.saveDIPackage(packageName);
            }
        });

        checkboxCreateLayout.addActionListener(e -> {
            comboSelectLayout.setEnabled(checkboxCreateLayout.isSelected());
        });

        buttonChooseBindingModule.addActionListener(e -> {
            String packageName = addFillPatternClassFilter(textBindingModule,LocalStorage.loadActivityBindingModule());
            if(packageName!=null){
                LocalStorage.saveActivityBindingModule(packageName);
            }
        });
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        contentPane.registerKeyboardAction(e -> {
            onOK();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED);
    }

    private void onOK() {
        String errorMessage = checkParams();
        if(errorMessage!=null){
            showError(errorMessage);
            return;
        }
        new WriteCommandAction(project){
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                LocalStorage.saveLayoutType(Constants.LAYOUT[comboSelectLayout.getSelectedIndex()]);
                params = new GenerateParams();
                params.setBasePackageName(textBasePackage.getText());
                params.setDiPackageName(textDIName.getText());
                params.setCreateLayout(checkboxCreateLayout.isSelected());
                String modulePath = file.getPath().substring(project.getBasePath().length());
                String temp = modulePath.replace("/",".");
                int packageIndex = temp.indexOf(Utils.readAndroidPackage(project));
                params.setGenerateClassPackageName(temp.substring(packageIndex));
                params.setDirectory(directory);
                params.setClassName(textActivityName.getText());
                params.setUseDataBinding(checkUseDataBinding.isSelected());
                params.setUsePresenter(checkUsePresenter.isSelected());
                params.setSpecifiedPresenter(comboPresenter.getSelectedItem().toString());
                params.setActivityBindingModuleClassName(textBindingModule.getText());
                presenter.generateActivity(project,params);
                resourcePresenter.generateActivityInManifest(project,params);
                if(checkboxCreateLayout.isSelected()){
                    params.setGenerateLayoutType(comboSelectLayout.getSelectedItem().toString());
                    resourcePresenter.generateLayout(project,params);
                }
            }
        }.execute();
        dispose();
    }

    private String checkParams(){
        String errorMessage = null;
        if(textActivityName.getText() == null||textActivityName.getText().trim().length()==0){
            return "Activity name can not be empty!";
        }
        if(textDIName.getText() == null||textDIName.getText().trim().length()==0){
            return "Dependency Inject class package should by selected";
        }
        if(textBasePackage.getText() == null||textBasePackage.getText().trim().length()==0){
            return "Dependency Inject class package should by selected";
        }
        if(textBindingModule.getText() == null|| textBindingModule.getText().trim().length()==0){
            return "Dependency Inject module package should by selected";
        }
        return errorMessage;
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    protected String addPackagePatternFilter(JTextField field,String defaultPackageName) {
        Module module = ModuleManager.getInstance(project).findModuleByName("app");
        PackageChooser chooser;
        if(module != null){
            chooser = new PackageChooserDialog(CodeInsightBundle.message("coverage.pattern.filter.editor.choose.package.title"), module);
        }else {
            chooser = new PackageChooserDialog(CodeInsightBundle.message("coverage.pattern.filter.editor.choose.package.title"), project);
        }
        if(defaultPackageName != null){
            chooser.selectPackage(defaultPackageName);
        }else {
            chooser.selectPackage(Utils.readAndroidPackage(project));
        }
        chooser.show();
        String packageName = null;
        if (chooser.isOK()) {
            PsiPackage packageItem = chooser.getSelectedPackage();
            if(packageItem!=null){
                packageName = packageItem.getQualifiedName();
                field.setText(packageName);
            }
        }
        return packageName;
    }

    public GenerateParams getParams(){
        return params;
    }

    private void showError(String error){
        Messages.showMessageDialog(error,"ERROR",Messages.getErrorIcon());
    }

    private TreeClassChooser initDefaultClassChooser(Module module){
        TreeClassChooser classChooser = TreeClassChooserFactory.getInstance(project).createProjectScopeChooser("Select Binding Class");
        if(module != null){
            VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(module.getModuleFilePath());
            PsiDirectory packageDir = PsiManager.getInstance(project).findDirectory(directory);
            classChooser.selectDirectory(packageDir);
        }
        return classChooser;
    }

    protected String addFillPatternClassFilter(JTextField field,String defaultName) {
        Module module = ModuleManager.getInstance(project).findModuleByName("app");
        if(defaultName != null && defaultName.length()>0){
            GlobalSearchScope scope = module!=null?GlobalSearchScope.moduleScope(module):GlobalSearchScope.projectScope(project);
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(defaultName, scope);
            if(psiClass!=null){
                classChooser = TreeClassChooserFactory.getInstance(project).
                        createNoInnerClassesScopeChooser("Select Binding Class", GlobalSearchScope.projectScope(project),
                                aClass -> true,psiClass);
                //workaround for version:173.x
                //https://intellij-support.jetbrains.com/hc/en-us/community/posts/360000611384-Plugin-not-work-properly-in-Android-Studio?page=1#community_comment_360000204784
                ApplicationManager.getApplication().invokeLater(() -> classChooser.select(psiClass), ModalityState.any());
//                classChooser.select(psiClass);
            }else {
                classChooser = initDefaultClassChooser(module);
            }
        }else {
            classChooser = initDefaultClassChooser(module);
        }
        classChooser.showDialog();
        PsiClass psiClass = classChooser.getSelected();
        if (psiClass != null) {
            field.setText(psiClass.getQualifiedName());
        }else {
            return null;
        }

        return psiClass.getQualifiedName();
    }
}
