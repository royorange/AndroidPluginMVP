package com.royorange.plugin.ui;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.ide.util.TreeClassChooserFactoryImpl;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
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

public class GenerateMVPFragment extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textFragmentName;
    private JTextField textBasePackage;
    private JButton buttonChooseBasePackage;
    private JTextField textDIPackage;
    private JButton buttonChooseDI;
    private JTextField textModuleClass;
    private JButton buttonChooseModule;
    private JComboBox comboPresenterType;
    private JComboBox comboSelectLayout;
    private JCheckBox checkboxCreateLayout;
    private JTextField textDIModule;
    private JButton buttonChooseDIModule;
    private Project project;
    private GenerateParams params;
    private VirtualFile file;
    private PsiDirectory directory;
    private MVPClassPresenter presenter;
    private ResourcePresenter resourcePresenter;
    private String modulePath;
    private boolean isFirstShow = true;

    public GenerateMVPFragment() {
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
        textDIPackage.setText(LocalStorage.loadDIPackage());
        textBasePackage.setText(LocalStorage.loadBasePackage());
        textModuleClass.setText(LocalStorage.loadHolderModule());
        setupSelectLayout();
        setupSelectPresenter();
    }

    private void setupSelectPresenter(){
        List<String> presenter = new ArrayList<>();
        int initIndex = 0;
        for(int i = 0; i < Constants.PRESENTER.length; i++){
            presenter.add(Constants.PRESENTER[i]);
        }
        ComboBoxModel<String> selectModel = new ListComboBoxModel<String>(presenter);
        comboPresenterType.setModel(selectModel);
        comboPresenterType.setSelectedIndex(initIndex);
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
            String packageName = addPackagePatternFilter(textDIPackage,LocalStorage.loadDIPackage());
            if(packageName!=null){
                LocalStorage.saveDIPackage(packageName);
            }
        });
        buttonChooseModule.addActionListener(e -> {
            String packageName = addFillPatternClassFilter(textModuleClass,LocalStorage.loadHolderModule());
            if(packageName!=null){
                LocalStorage.saveHolderModule(packageName);
            }
        });

        checkboxCreateLayout.addActionListener(e -> {
            comboSelectLayout.setEnabled(checkboxCreateLayout.isSelected());
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
                params.setDiPackageName(textDIPackage.getText());
                params.setCreateLayout(checkboxCreateLayout.isSelected());
                String modulePath = file.getPath().substring(project.getBasePath().length());
                String temp = modulePath.replace("/",".");
                int packageIndex = temp.indexOf(Utils.readAndroidPackage(project));
                params.setGenerateClassPackageName(temp.substring(packageIndex));
                params.setDirectory(directory);
                params.setClassName(textFragmentName.getText());
                params.setSpecifiedPresenter(comboPresenterType.getSelectedItem().toString());
                params.setModuleName(textModuleClass.getText());
                presenter.generateFragment(project,params);
                if(checkboxCreateLayout.isSelected()){
                    params.setGenerateLayoutType(comboSelectLayout.getSelectedItem().toString());
                    resourcePresenter.generateLayout(project,params);
                }
            }
        }.execute();
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    protected String addPackagePatternFilter(JTextField field,String defaultPackageName) {
        PackageChooser chooser;
        Module module = ModuleManager.getInstance(project).findModuleByName("app");
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

    private TreeClassChooser initDefaultClassChooser(Module module){
        TreeClassChooser classChooser = TreeClassChooserFactory.getInstance(project).createProjectScopeChooser("Select Presenter Class");
        if(module != null){
            VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(module.getModuleFilePath());
            PsiDirectory packageDir = PsiManager.getInstance(project).findDirectory(directory);
            if(packageDir!=null){
                classChooser.selectDirectory(packageDir);
            }
        }
        return classChooser;
    }

    protected String addFillPatternClassFilter(JTextField field,String defaultName) {
        TreeClassChooser classChooser;
        Module module = ModuleManager.getInstance(project).findModuleByName("app");
        if(defaultName != null && defaultName.length()>0){
            GlobalSearchScope scope = module!=null?GlobalSearchScope.moduleScope(module):GlobalSearchScope.projectScope(project);
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(defaultName, scope);
            if(psiClass!=null){
                classChooser = TreeClassChooserFactory.getInstance(project).
                        createNoInnerClassesScopeChooser("Select Presenter Class", GlobalSearchScope.projectScope(project),
                                aClass -> true,psiClass);
                classChooser.select(psiClass);
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

    private String checkParams(){
        String errorMessage = null;
        if(textFragmentName.getText() == null||textFragmentName.getText().trim().length()==0){
            return "Fragment name can not be empty!";
        }
        if(textModuleClass.getText() == null||textModuleClass.getText().trim().length()==0){
            return "Module should be specified";
        }
        if(textDIPackage.getText() == null||textDIPackage.getText().trim().length()==0){
            return "Dependency Inject class package should by selected";
        }
        if(textBasePackage.getText() == null||textBasePackage.getText().trim().length()==0){
            return "Dependency Inject class package should by selected";
        }
        return errorMessage;
    }

    private void showError(String error){
        Messages.showMessageDialog(error,"ERROR",Messages.getErrorIcon());
    }
}
