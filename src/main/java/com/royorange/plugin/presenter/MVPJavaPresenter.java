package com.royorange.plugin.presenter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.royorange.plugin.model.GenerateParams;

import java.util.ArrayList;
import java.util.List;

public class MVPJavaPresenter extends PresenterImpl {
    private static final String DI_PACKAGE = "com.arvato.sephora.app.presentation.internal.di";
    private static final String BASE_PACKAGE = "com.arvato.sephora.app.presentation.view.base";

    Project project;
    PsiElementFactory elementFactory;


    @Override
    public void generate(Project project,PsiFile file) {
        this.project = project;
        elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        generate((PsiJavaFile)file);
    }

    @Override
    public void generateActivity(Project project, GenerateParams params) {
        this.project = project;
        elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        String packageName = readPackage();
        String prefix = params.getClassName();
        int suffixIndex = params.getClassName().lastIndexOf("Activity");
        if(suffixIndex >= 0){
            prefix =  params.getClassName().substring(0,suffixIndex);
        }
        PsiClass activityClass = createActivity(packageName,prefix,params);
        PsiFile activityFile = activityClass.getContainingFile();
        createContract(prefix,activityFile.getContainingDirectory());
        createPresenter(prefix,activityFile.getContainingDirectory());
        createModule(prefix,activityFile.getContainingDirectory());
        updateActivityBindingModule(prefix,params.getDiPackageName(),activityClass);
    }

    @Override
    public void generateFragment(Project project, GenerateParams params) {
        this.project = project;
        elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    public void generate(PsiJavaFile file) {
        PsiClass psiClass = file.getClasses()[0];
//        boolean isActivity = InheritanceUtil.isInheritor(psiClass,"android.app.Activity");
        boolean isActivity = InheritanceUtil.isInheritor(psiClass,"com.roy.Activity")
                ||psiClass.getName().endsWith("Activity");
        if(isActivity){
            generateActivityProcess(file,psiClass);
        }
        boolean isFragment = InheritanceUtil.isInheritor(psiClass,"com.roy.Fragment")
                ||psiClass.getName().endsWith("Fragment");
        if(isFragment){
            generateFragmentProcess(file,psiClass);
        }
    }

    private void generateActivityProcess(PsiJavaFile file,PsiClass activityClass){
        String prefix;
        String className = activityClass.getName();
        int end = className.indexOf("Activity");
        if(end > 0){
            prefix = className.substring(0,end);
        }else {
            prefix = className;
        }

        createContract(prefix,file.getContainingDirectory());
        createPresenter(prefix,file.getContainingDirectory());
        createModule(prefix,file.getContainingDirectory());
        String contractName = prefix + "Contract";
        if(activityClass.getExtendsList().getReferencedTypes().length == 0){
            StringBuilder stringBuilder = new StringBuilder("BaseActivity<");
            addImport(file,BASE_PACKAGE+".BaseActivity");
            stringBuilder.append(contractName).append(".Presenter,DataBinding>");
            activityClass.getExtendsList().add(elementFactory.createReferenceFromText(stringBuilder.toString(),activityClass));
        }
        if(activityClass.getImplementsList().getReferencedTypes().length == 0){
            activityClass.getImplementsList().add(elementFactory.createReferenceFromText(contractName + ".View",activityClass));
        }
    }

    private String readPackage(){
        VirtualFile manifestVirtualFile = LocalFileSystem.getInstance().findFileByPath(project.getBasePath()+"/app/src/main/AndroidManifest.xml");
        String packageName = "";
        if(manifestVirtualFile!=null){
            XmlFile manifestFile = (XmlFile)PsiManager.getInstance(project).findFile(manifestVirtualFile);
            XmlAttribute aPackage = manifestFile.getRootTag().getAttribute("package");
            if(aPackage!=null){
                packageName = aPackage.getValue();
            }
        }

        return packageName;
    }

    private PsiClass createActivity(String packageName,String prefix,GenerateParams params){
        PsiClass activityClass = createClassFileIfNotExist(params.getClassName(),"",params.getDirectory());
        PsiFile activityFile = activityClass.getContainingFile();
        List<String> importList = new ArrayList<>();
        importList.add("android.os.Bundle");
        StringBuilder stringBuilder = new StringBuilder();
        String contractName = prefix + "Contract";
        if(params.isUseDataBinding()){
            String bindingName = "Activity" + prefix + "Binding";
            importList.add(packageName + ".databinding." + bindingName);
            if(params.isUsePresenter()){
                importList.add(params.getBasePackageName()+".BaseActivity");
                stringBuilder.append("BaseActivity<").append(contractName).append(".Presenter,")
                .append(bindingName).append(">");
            }else {
                importList.add(params.getBasePackageName()+".DataBindingActivity");
                stringBuilder.append("DataBindingActivity<").append(bindingName).append(">");
            }
        }else {
            importList.add(params.getBasePackageName()+".PureActivity");
        }
        String extendedClass = stringBuilder.toString();
        if(extendedClass.length() > 0){
            activityClass.getExtendsList().add(elementFactory.createReferenceFromText(extendedClass,activityClass));
            activityClass.getImplementsList().add(elementFactory.createReferenceFromText(contractName + ".View",activityClass));
        }else {
            activityClass.getExtendsList().add(elementFactory.createReferenceFromText("PureActivity",activityClass));
        }
        if(params.isCreateLayout()){
            importList.add(packageName + ".R");
        }
        addImport((PsiJavaFile) activityFile.getContainingFile(),importList);
        generateActivityBasicMethod(activityClass,prefix,params.isCreateLayout());

        return activityClass;
    }

    private void generateActivityBasicMethod(PsiClass activityClass,String prefix,boolean isAutoCreateLayout){
        PsiMethod createMethod = elementFactory.createMethodFromText("protected void onCreate(Bundle savedInstanceState) {super.onCreate(savedInstanceState);}",activityClass);
        createMethod.getModifierList().addAnnotation("Override");
        activityClass.add(createMethod);
        if(isAutoCreateLayout){
            PsiMethod contentViewMethod = elementFactory.createMethodFromText("protected int getContentViewId() {}",activityClass);
            StringBuilder stringBuilder = new StringBuilder("return R.layout.activity");
            int lastIndex=0;
            for(int i=0;i<prefix.length();i++){

                if(i>0){
                    char character = prefix.charAt(i);
                    //split by upper character
                    if(character>='A' && character<='Z'){
                        stringBuilder.append("_").append(prefix.substring(lastIndex,i).toLowerCase());
                        lastIndex = i;
                    }
                }
            }
            if(lastIndex>0){
                stringBuilder.append("_"+prefix.substring(lastIndex).toLowerCase());
            }
            stringBuilder.append(";");
            contentViewMethod.getBody().add(elementFactory.createStatementFromText(stringBuilder.toString(),activityClass));
//            PsiMethod contentViewMethod = elementFactory.createMethodFromText(stringBuilder.toString(),activityClass);
            contentViewMethod.getModifierList().addAnnotation("Override");
            activityClass.add(contentViewMethod);
        }
    }

    private void generateFragmentProcess(PsiJavaFile file,PsiClass fragmentClass){
        String prefix;
        String className = fragmentClass.getName();
        int end = className.indexOf("Fragment");
        if(end > 0){
            prefix = className.substring(0,end);
        }else {
            prefix = className;
        }

        createContract(prefix,file.getContainingDirectory());
        createPresenter(prefix,file.getContainingDirectory());
        if(fragmentClass.getExtendsList().getReferencedTypes().length == 0){
            addImport(file,BASE_PACKAGE+".BaseFragment");
            StringBuilder stringBuilder = new StringBuilder("BaseFragment<");
            stringBuilder.append(prefix).append("Contract.Presenter,DataBinding>");
            fragmentClass.getExtendsList().add(elementFactory.createReferenceFromText(stringBuilder.toString(),fragmentClass));
        }
        if(fragmentClass.getImplementsList().getReferencedTypes().length == 0){
            String contractName = prefix + "Contract.View";
            fragmentClass.getImplementsList().add(elementFactory.createReferenceFromText(contractName,fragmentClass));
        }
    }

    private void createContract(String prefix,PsiDirectory directory){
        PsiClass contract = createClassFileIfNotExist(prefix,"Contract",directory);
        //add View
        addInnerInterface(contract,"View","BaseView");
        //add Presenter
        addInnerInterface(contract,"Presenter","BasePresenter","View");
    }

    private void createPresenter(String prefix,PsiDirectory directory){
        PsiClass presenter = createClassFileIfNotExist(prefix,"Presenter",directory);
        String contractView = prefix + "Contract.View";
        String contractPresenter = prefix + "Contract.Presenter";
        addImport((PsiJavaFile)presenter.getContainingFile(),BASE_PACKAGE+".RxPresenter");
        if(presenter.getExtendsList().getReferencedTypes().length == 0){
            String basePresenterName = "RxPresenter<"+ contractView + ">";
            presenter.getExtendsList().add(elementFactory.createReferenceFromText(basePresenterName,presenter));
        }
        if(presenter.getImplementsList().getReferencedTypes().length == 0){
            presenter.getImplementsList().add(elementFactory.createReferenceFromText(contractPresenter,presenter));
        }
    }

    private void createModule(String prefix,PsiDirectory directory){
        PsiClass module = createClassFileIfNotExist(prefix,"PresenterModule",directory);
        List<String> importList = new ArrayList<>();
        importList.add("dagger.Binds");
        importList.add("dagger.Module");
        importList.add(DI_PACKAGE+".scope.ActivityScoped");
        addImport((PsiJavaFile)module.getContainingFile(),importList);
        module.getModifierList().setModifierProperty(PsiModifier.ABSTRACT,true);
        if(module.getModifierList().getAnnotations().length==0){
            module.getModifierList().addAnnotation("Module");
        }
        generatePresenterInModule(prefix,module);
    }

    private void generatePresenterInModule(String prefix,PsiClass module){
        String presenterName = prefix.substring(0,1).toLowerCase()+prefix.substring(1) + "Presenter";
        //already exist
        if(module.findMethodsByName(presenterName,false).length>0){
            return;
        }
        String presenterClassName = prefix + "Presenter";
        String contractName = prefix + "Contract";
        StringBuilder stringBuilder = new StringBuilder("abstract ");
        stringBuilder.append(contractName)
                .append(".Presenter ")
                .append(presenterName)
                .append("(")
                .append(presenterClassName)
                .append(" ").append(presenterName).append(");");
        PsiMethod method = elementFactory.createMethodFromText(stringBuilder.toString(),module);
        method.getModifierList().addAnnotation("Binds");
        method.getModifierList().addAnnotation("ActivityScoped");
        module.add(method);
    }

    private void addInnerInterface(PsiClass psiClass,String className,String extendedFrom){
        addInnerInterface(psiClass,className,extendedFrom,null);
    }

    private void addInnerInterface(PsiClass psiClass,String className,String extendedFrom,String generics){
        if(psiClass.findInnerClassByName(className,true)!=null){
            return;
        }
        PsiClass anInterface = elementFactory.createInterface(className);
        String superName = extendedFrom;
        if(generics!=null&&generics.length()>0){
            superName = extendedFrom +"<" + generics + ">";
        }
        anInterface.getExtendsList().add(elementFactory.createReferenceFromText(superName,anInterface));
        //delete default added public modifier for interface
        anInterface.getModifierList().getChildren()[0].delete();
        addImport((PsiJavaFile)psiClass.getContainingFile(),BASE_PACKAGE + "." + extendedFrom);
        psiClass.add(anInterface);
    }

    private PsiFile findJavaExist(PsiDirectory directory,String name){
        return directory.findFile(name+".java");
    }

    private PsiClass createClassFileIfNotExist(String prefix,String functionName,PsiDirectory directory){
        return createClassFileIfNotExist(prefix,functionName,null,directory);
    }

    private PsiClass createClassFileIfNotExist(String prefix,String functionName,String annotation,PsiDirectory directory){
        String className = prefix + functionName;
        PsiClass classFile;
        PsiFile targetFile = findJavaExist(directory,className);
        if(targetFile!=null){
            classFile = ((PsiJavaFile)targetFile).getClasses()[0];
        }else {
            classFile = JavaDirectoryService.getInstance().createClass(directory,className);
        }
        if(annotation != null){
            if(!annotation.equals("@")){
                annotation = "@" + annotation;
            }
            classFile.getModifierList().addAnnotation(annotation);
        }
        return classFile;
    }

    private PsiElement addImport(PsiJavaFile javaFile, String fullyQualifiedName) {

        final PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return null;
        }

        // Check if already imported
        for (PsiImportStatement is : importList.getImportStatements()) {
            String impQualifiedName = is.getQualifiedName();
            if (fullyQualifiedName.equals(impQualifiedName)) {
                return null; // Already imported so nothing needed
            }
        }
        // Not imported yet so add it
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullyQualifiedName,javaFile.getResolveScope());
        PsiElement element;
        if(psiClass!=null){
            element = importList.add(elementFactory.createImportStatement(psiClass));
        }else {
            element = importList.add(elementFactory.createImportStatementOnDemand(fullyQualifiedName));
        }
        return element;
    }

    private PsiElement addImport(PsiJavaFile javaFile, List<String> fullyQualifiedName) {
        final PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return null;
        }
        PsiElement element = null;
        // Check if already imported
        for(String name:fullyQualifiedName){
            String needAdd = name;
            for (PsiImportStatement is : importList.getImportStatements()) {
                String impQualifiedName = is.getQualifiedName();
                if (name.equals(impQualifiedName)) {
                    needAdd = null;
                    break;
                }else {
                    needAdd = name;
                }
            }
            // Not imported yet so add it
            if(needAdd!=null){
                PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(needAdd,javaFile.getResolveScope());
                if(psiClass!=null){
                    element = importList.add(elementFactory.createImportStatement(psiClass));
                }else {
//                    if(!needAdd.startsWith("import")){
//                        needAdd = "import " + needAdd;
//                    }
//                    if(!needAdd.endsWith(";")){
//                        needAdd+=";";
//                    }
//                    element = importList.add(elementFactory.createStatementFromText(needAdd,psiClass));
                    element = importList.add(elementFactory.createImportStatementOnDemand(needAdd));
                }
            }
        }
        return element;
    }

    private void updateActivityBindingModule(String prefix,String diPackage,PsiClass activityClass){
        String bindingDirector = project.getBasePath()+"/app/src/main/java/"+diPackage.replace(".","/")+"/module/";
        VirtualFile virtualDirector = LocalFileSystem.getInstance().findFileByPath(bindingDirector);
        PsiClass activityBindingModule = null;
        if(virtualDirector == null){
            //module not found
        }else {
            VirtualFile bindVirtual = LocalFileSystem.getInstance().findFileByPath(bindingDirector+"ActivityBindingModule.java");
            if(bindVirtual == null){
                //create file
                activityBindingModule = JavaDirectoryService.getInstance().createClass(PsiManager.getInstance(project).findDirectory(virtualDirector),"ActivityBindingModule");
            }else {
                activityBindingModule = ((PsiJavaFile)PsiManager.getInstance(project).findFile(bindVirtual)).getClasses()[0];
            }
        }


        if(!activityBindingModule.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT)){
            activityBindingModule.getModifierList().setModifierProperty(PsiModifier.ABSTRACT,true);
        }
        List<String> importList = new ArrayList<>();
        importList.add(diPackage+".scope.ActivityScoped");
        importList.add("dagger.Module");
        importList.add("dagger.android.ContributesAndroidInjector");
        String apiName = activityClass.getName();
        if(apiName.length()>1){
            apiName = apiName.substring(0,1).toLowerCase()+apiName.substring(1);
        }

        StringBuilder sb = new StringBuilder("@ActivityScoped\n" +
                "    @ContributesAndroidInjector (modules = ")
                .append(prefix)
                .append("Module.class)\n" +
                        "    abstract ")
                .append(activityClass.getName())
                .append(" ")
                .append(apiName)
                .append("();");
        PsiMethod method = elementFactory.createMethodFromText(sb.toString(),activityClass);
        addImport((PsiJavaFile) activityBindingModule.getContainingFile(),importList);
        activityBindingModule.add(method);
    }
}
