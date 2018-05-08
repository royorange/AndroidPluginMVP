package com.royorange.plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;

/**
 * Created by Roy on 17/5/27.
 */
public class Utils {

    public static PsiClass getClass(PsiFile file){
        PsiClass currentClass = null;
        if(file instanceof PsiJavaFile){
            currentClass = ((PsiJavaFile) file).getClasses()[0];
        }
        return currentClass;
    }

    public static boolean checkMethodExist(PsiClass psiClass, String methodName){
        return checkMethodExist(psiClass,methodName,null);
    }


    public static boolean checkMethodExist(PsiClass psiClass, String methodName, String... arguments){
        boolean exist = false;
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        for (PsiMethod method : methods) {
            if(arguments == null){
                exist = true;
                break;
            }
            PsiParameterList parameterList = method.getParameterList();

            if (parameterList.getParametersCount() == arguments.length) {
                boolean isEquat = true;
                PsiParameter[] parameters = parameterList.getParameters();
                for (int i = 0; i < arguments.length; i++) {
                    if (!parameters[i].getType().getCanonicalText().equals(arguments[i])) {
                        isEquat = false;
                    }
                }

                if (isEquat) {
                    exist = true;
                }
            }
        }
        return exist;
    }

    public static String readAndroidPackage(Project project){
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

    public static String getAndroidManifestPath(Project project){
        return project.getBasePath()+"/app/src/main/AndroidManifest.xml";
    }

    public static String getAndroidLayoutPath(Project project){
        return project.getBasePath()+"/app/src/main/res/layout";
    }

    public static String splitClassByUnderline(String prefix,String name){
        StringBuilder stringBuilder = new StringBuilder(prefix);
        int lastIndex=0;
        for(int i=0;i<name.length();i++){

            if(i>0){
                char lastCharacter = name.charAt(i-1);
                char character = name.charAt(i);
                //split by upper character
                if(character>='A' && character<='Z'&&lastCharacter>='a'&&lastCharacter<='z'){
                    stringBuilder.append("_").append(name.substring(lastIndex,i).toLowerCase());
                    lastIndex = i;
                }
            }
        }
        stringBuilder.append("_"+name.substring(lastIndex).toLowerCase());
        return stringBuilder.toString();
    }
}
