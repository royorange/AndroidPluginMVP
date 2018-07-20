package com.royorange.plugin.presenter;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.royorange.plugin.model.GenerateParams;
import com.royorange.plugin.util.LocalStorage;
import com.royorange.plugin.util.Utils;

import java.util.ArrayList;
import java.util.List;

public class MVPJavaPresenter extends PresenterImpl {

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

        if(params.isUsePresenter()){
            createContract(prefix,activityFile.getContainingDirectory());
            createPresenter(prefix,activityFile.getContainingDirectory());
            createModule(prefix,activityFile.getContainingDirectory());
        }

        Module currentModule = ProjectFileIndex.getInstance(project).getModuleForFile(activityFile.getVirtualFile());
        updateActivityBindingModule(prefix,params,activityClass,currentModule);
    }

    @Override
    public void generateFragment(Project project, GenerateParams params) {
        this.project = project;
        elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
        String packageName = readPackage();
        String prefix = params.getClassName();
        int suffixIndex = params.getClassName().lastIndexOf("Fragment");
        if(suffixIndex >= 0){
            prefix =  params.getClassName().substring(0,suffixIndex);
        }
        PsiClass fragmentClass = createFragment(packageName,prefix,params);
        PsiFile fragmentFile = fragmentClass.getContainingFile();
        PsiClass contractClass = createContract(prefix,fragmentFile.getContainingDirectory());
        PsiClass presenterClass = createPresenter(prefix,fragmentFile.getContainingDirectory());
        updateModule(params,contractClass,presenterClass);
        generateFragmentBasicMethod(fragmentClass,prefix,params.isCreateLayout());
    }

    public void generate(PsiJavaFile file) {
        PsiClass psiClass = file.getClasses()[0];
//        boolean isActivity = InheritanceUtil.isInheritor(psiClass,"android.app.Activity");
        boolean isActivity = InheritanceUtil.isInheritor(psiClass,"android.app.Activity")
                ||psiClass.getName().endsWith("Activity");
        if(isActivity){
            generateActivityProcess(file,psiClass);
        }
        boolean isFragment = InheritanceUtil.isInheritor(psiClass,"android.support.v4.app.Fragment")
                ||InheritanceUtil.isInheritor(psiClass,"android.app.Fragment")
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
            addImport(file,LocalStorage.loadBasePackage() +".BaseActivity");
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
        importList.add("android.content.Intent");
        importList.add("android.content.Context");
        StringBuilder stringBuilder = new StringBuilder();
        String contractName = prefix + "Contract";
        if(params.isUseDataBinding()){
            //only the first character is upper case
            String bindingName = "Activity" + prefix + "Binding";
            importList.add(packageName + ".databinding." + bindingName);
            if(params.isUsePresenter()){
                importList.add(params.getBasePackageName()+".BaseActivity2");
                stringBuilder.append("BaseActivity2<").append(contractName).append(".Presenter,")
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
            StringBuilder stringBuilder = new StringBuilder("return R.layout.");
            stringBuilder.append(Utils.splitClassByUnderline("activity",prefix));
            stringBuilder.append(";");
            contentViewMethod.getBody().add(elementFactory.createStatementFromText(stringBuilder.toString(),activityClass));
//            PsiMethod contentViewMethod = mElementFactory.createMethodFromText(stringBuilder.toString(),activityClass);
            contentViewMethod.getModifierList().addAnnotation("Override");
            activityClass.add(contentViewMethod);
        }
        //generate intent
        StringBuilder intentBuilder = new StringBuilder("Intent callingIntent = new Intent(context,");
        intentBuilder.append(activityClass.getName()).append(".class);");
        PsiMethod intentMethod = elementFactory.createMethodFromText("public static Intent getCallingIntent(Context context) {}",activityClass);
        intentMethod.getBody().add(elementFactory.createStatementFromText(intentBuilder.toString(),activityClass));
        intentMethod.getBody().add(elementFactory.createStatementFromText("return callingIntent;",activityClass));
        activityClass.add(intentMethod);
    }

    private void generateFragmentBasicMethod(PsiClass activityClass,String prefix,boolean isAutoCreateLayout){
        if(isAutoCreateLayout){
            PsiMethod contentViewMethod = elementFactory.createMethodFromText("protected int getContentViewId() {}",activityClass);
            StringBuilder stringBuilder = new StringBuilder("return R.layout.");
            stringBuilder.append(Utils.splitClassByUnderline("fragment",prefix));
            stringBuilder.append(";");
            contentViewMethod.getBody().add(elementFactory.createStatementFromText(stringBuilder.toString(),activityClass));
//            PsiMethod contentViewMethod = mElementFactory.createMethodFromText(stringBuilder.toString(),activityClass);
            contentViewMethod.getModifierList().addAnnotation("Override");
            activityClass.add(contentViewMethod);
        }
    }

    private PsiClass createFragment(String packageName,String prefix,GenerateParams params){
        PsiClass fragmentClass = createClassFileIfNotExist(params.getClassName(),"",params.getDirectory());
        PsiFile fragmentFile = fragmentClass.getContainingFile();
        List<String> importList = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        String contractName = prefix + "Contract";
        String bindingName = "Fragment" + prefix + "Binding";
        importList.add(packageName + ".databinding." + bindingName);
        importList.add(params.getBasePackageName()+".BaseFragment2");
        stringBuilder.append("BaseFragment2<").append(contractName).append(".Presenter,")
                .append(bindingName).append(">");
        String extendedClass = stringBuilder.toString();
        fragmentClass.getExtendsList().add(elementFactory.createReferenceFromText(extendedClass,fragmentClass));
        fragmentClass.getImplementsList().add(elementFactory.createReferenceFromText(contractName + ".View",fragmentClass));
        importList.add(packageName + ".R");
        importList.add("javax.inject.Inject");
        addImport((PsiJavaFile) fragmentFile.getContainingFile(),importList);

        PsiMethod constructorMethod = elementFactory.createConstructor(fragmentClass.getName());
        constructorMethod.getModifierList().addAnnotation("Inject");
        fragmentClass.add(constructorMethod);
        return fragmentClass;
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
            addImport(file,LocalStorage.loadBasePackage()+".BaseFragment");
            StringBuilder stringBuilder = new StringBuilder("BaseFragment<");
            stringBuilder.append(prefix).append("Contract.Presenter,DataBinding>");
            fragmentClass.getExtendsList().add(elementFactory.createReferenceFromText(stringBuilder.toString(),fragmentClass));
        }
        if(fragmentClass.getImplementsList().getReferencedTypes().length == 0){
            String contractName = prefix + "Contract.View";
            fragmentClass.getImplementsList().add(elementFactory.createReferenceFromText(contractName,fragmentClass));
        }
    }

    private PsiClass createContract(String prefix,PsiDirectory directory){
        PsiClass contract = createClassFileIfNotExist(prefix,"Contract",directory);
        //add View
        addInnerInterface(contract,"View","BaseView");
        //add Presenter
        addInnerInterface(contract,"Presenter","BasePresenter","View");
        return contract;
    }

    private PsiClass createPresenter(String prefix,PsiDirectory directory){
        PsiClass presenter = createClassFileIfNotExist(prefix,"Presenter",directory);
        String contractView = prefix + "Contract.View";
        String contractPresenter = prefix + "Contract.Presenter";
        addImport((PsiJavaFile)presenter.getContainingFile(),LocalStorage.loadBasePackage()+".RxPresenter");
        addImport((PsiJavaFile)presenter.getContainingFile(),"javax.inject.Inject");
        if(presenter.getExtendsList().getReferencedTypes().length == 0){
            String basePresenterName = "RxPresenter<"+ contractView + ">";
            presenter.getExtendsList().add(elementFactory.createReferenceFromText(basePresenterName,presenter));
        }
        if(presenter.getImplementsList().getReferencedTypes().length == 0){
            presenter.getImplementsList().add(elementFactory.createReferenceFromText(contractPresenter,presenter));
        }

        PsiMethod constructorMethod = elementFactory.createConstructor(presenter.getName());
        constructorMethod.getModifierList().addAnnotation("Inject");
        presenter.add(constructorMethod);
        return presenter;
    }

    private void createModule(String prefix,PsiDirectory directory){
        PsiClass module = createClassFileIfNotExist(prefix,"PresenterModule",directory);
        List<String> importList = new ArrayList<>();
        importList.add("dagger.Binds");
        importList.add("dagger.Module");
        importList.add(LocalStorage.loadDIPackage()+".scope.ActivityScoped");
        addImport((PsiJavaFile)module.getContainingFile(),importList);
        module.getModifierList().setModifierProperty(PsiModifier.ABSTRACT,true);
        if(module.getModifierList().getAnnotations().length==0){
            module.getModifierList().addAnnotation("Module");
        }
        generatePresenterInModule(prefix,module);
    }

    private void updateModule(GenerateParams params,PsiClass contractClass,PsiClass presenterClass){
        Module currentModule = ProjectFileIndex.getInstance(project).getModuleForFile(presenterClass.getContainingFile().getVirtualFile());
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(params.getModuleName(), GlobalSearchScope.moduleScope(currentModule));

        PsiJavaFile psiJavaFile = (PsiJavaFile)psiClass.getContainingFile();
        if(psiClass!=null){
            List<String> importList = new ArrayList<>();
            String modulePackage = psiJavaFile.getPackageName();
            if(!modulePackage.equals(((PsiJavaFile)contractClass.getContainingFile()).getPackageName())){
                importList.add(contractClass.getQualifiedName());
            }
            if(!modulePackage.equals(((PsiJavaFile)presenterClass.getContainingFile()).getPackageName())){
                importList.add(presenterClass.getQualifiedName());
            }
            importList.add(params.getDiPackageName()+".scope.FragmentScoped");
            importList.add("dagger.android.ContributesAndroidInjector");
            //add import
            addImport(psiJavaFile,importList);
            String methodName = params.getClassName();
            if(methodName.length()>1){
                methodName = methodName.substring(0,1)+methodName.substring(1);
            }else {
                methodName.toLowerCase();
            }
            StringBuilder sb = new StringBuilder("@FragmentScoped\n" +
                    "    @ContributesAndroidInjector");
            sb.append("\n    abstract ")
                    .append(params.getClassName())
                    .append(" ")
                    .append(methodName)
                    .append("();");
            PsiMethod method = elementFactory.createMethodFromText(sb.toString(),psiClass);
            psiClass.add(method);
        }
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
        addImport((PsiJavaFile)psiClass.getContainingFile(),LocalStorage.loadBasePackage() + "." + extendedFrom);
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
            classFile.getModifierList().setModifierProperty(PsiModifier.PUBLIC,true);
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
//                    element = importList.add(mElementFactory.createStatementFromText(needAdd,psiClass));
                    element = importList.add(elementFactory.createImportStatementOnDemand(needAdd));
                }
            }
        }
        return element;
    }

    private void updateActivityBindingModule(String prefix,GenerateParams params,PsiClass activityClass,Module currentModule){
        PsiClass activityBindingModule = JavaPsiFacade.getInstance(project).findClass(params.getActivityBindingModuleClassName(),GlobalSearchScope.moduleScope(currentModule));
        if(activityBindingModule == null){
            String bindingDirector = project.getBasePath()+"/app/src/main/java/"+params.getDiPackageName().replace(".","/")+"/modules/";
            VirtualFile virtualDirector = LocalFileSystem.getInstance().findFileByPath(bindingDirector);
//            VirtualFile bindVirtual = LocalFileSystem.getInstance().findFileByPath(bindingDirector+"ActivityBindingModule.java");
            //create file
            activityBindingModule = JavaDirectoryService.getInstance().createClass(PsiManager.getInstance(project).findDirectory(virtualDirector),"ActivityBindingModule");
            activityBindingModule.getModifierList().setModifierProperty(PsiModifier.PUBLIC,true);
            activityBindingModule.getModifierList().setModifierProperty(PsiModifier.ABSTRACT,true);
            activityBindingModule.getModifierList().addAnnotation("Module");
        }

        List<String> importList = new ArrayList<>();
        importList.add(params.getDiPackageName()+".scope.ActivityScoped");
        importList.add("dagger.Module");
        importList.add("dagger.android.ContributesAndroidInjector");
        String packageName = activityClass.getQualifiedName().substring(0,activityClass.getQualifiedName().length()-activityClass.getName().length());
        //import activity
        importList.add(activityClass.getQualifiedName());
        //import module
        importList.add(packageName+prefix+"PresenterModule");
        String apiName = activityClass.getName();
        if(apiName.length()>1){
            apiName = apiName.substring(0,1).toLowerCase()+apiName.substring(1);
        }

        StringBuilder sb = new StringBuilder("@ActivityScoped\n" +
                "    @ContributesAndroidInjector");
                if(params.isUseDataBinding()){
                    sb.append(" (modules=")
                      .append(prefix).append("PresenterModule.class)");
                }
                sb.append("\n    abstract ")
                .append(activityClass.getName())
                .append(" ")
                .append(apiName)
                .append("();");
        PsiMethod method = elementFactory.createMethodFromText(sb.toString(),activityClass);
        addImport((PsiJavaFile) activityBindingModule.getContainingFile(),importList);
        activityBindingModule.add(method);
    }

}
