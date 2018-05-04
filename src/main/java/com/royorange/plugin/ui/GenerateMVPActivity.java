package com.royorange.plugin.ui;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.PackageChooser;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import com.royorange.plugin.model.GenerateParams;
import com.royorange.plugin.presenter.MVPClassPresenter;
import com.royorange.plugin.presenter.MVPJavaPresenter;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
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
    private JSpinner spinnerSelectLayout;
    private JComboBox comboSelectLayout;
    private Project project;
    private GenerateParams params;
    private VirtualFile file;
    private PsiDirectory directory;
    private MVPClassPresenter presenter;

    private final String[] LAYOUT = {"CoordinatorLayout","ConstraintLayout","FrameLayout","LinearLayout","RelativeLayout"};

    public GenerateMVPActivity() {
        presenter = new MVPJavaPresenter();
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
        setVisible(true);
    }

    private void initView(){
//        JSpinner.ListEditor spinnerEditor = new JSpinner.ListEditor(spinnerSelectLayout);
        SpinnerListModel model = new SpinnerListModel(LAYOUT);
        List<String> layout = new ArrayList<>();
        for(int i=0;i < LAYOUT.length;i++){
            layout.add(LAYOUT[i]);
        }
        model.setList(layout);
        ComboBoxModel<String> layoutModel = new ListComboBoxModel<String>(layout);
        comboSelectLayout.setModel(layoutModel);
//        spinnerSelectLayout.setModel(model);
//        spinnerSelectLayout.setEditor(spinnerEditor);
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
            addPatternFilter(textBasePackage);
        });

        buttonChooseDI.addActionListener(e -> {
            addPatternFilter(textDIName);
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
    }

    private void onOK() {
        // add your code here
        if(textActivityName.getText() == null||textActivityName.getText().trim().length()==0){
            return;
        }
        new WriteCommandAction(project){
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                params = new GenerateParams();
                params.setBasePackageName(textBasePackage.getText());
                params.setDiPackageName(textDIName.getText());
                params.setCreateLayout(checkboxCreateLayout.isSelected());
                params.setGenerateClassPackageName(file.getName());
                params.setDirectory(directory);
                params.setClassName(textActivityName.getText());
                params.setUseDataBinding(checkUseDataBinding.isSelected());
                params.setUsePresenter(checkUsePresenter.isSelected());
                presenter.generateActivity(project,params);
            }
        }.execute();
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    protected void addPatternFilter(JTextField field) {
        PackageChooser chooser = new PackageChooserDialog(CodeInsightBundle.message("coverage.pattern.filter.editor.choose.package.title"), project);
        chooser.show();
        if (chooser.isOK()) {
            PsiPackage packageItem = chooser.getSelectedPackage();
            if(packageItem!=null){
                field.setText(packageItem.getQualifiedName());
            }
        }
    }

    public GenerateParams getParams(){
        return params;
    }

}
