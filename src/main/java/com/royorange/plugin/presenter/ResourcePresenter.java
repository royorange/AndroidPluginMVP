package com.royorange.plugin.presenter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.royorange.plugin.constant.Constants;
import com.royorange.plugin.model.GenerateParams;
import com.royorange.plugin.util.Utils;

public class ResourcePresenter {
    private static final String DEFAULT_ATTR = "xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"";
    Project mProject;

    public ResourcePresenter() {
    }

    public void generateActivityInManifest(Project project, GenerateParams params){
        this.mProject = project;
        addActivityInManifest(params.getGenerateClassPackageName(),params.getClassName());
    }

    public void generateLayout(Project project, GenerateParams params){
        this.mProject = project;
        XmlFile file = createXmlIfNotExist(params);
        String name = params.getGenerateLayoutType();
        if(params.isUseDataBinding()){
            XmlTag tag = getDataBindingTag();
            tag.add(getLayoutTag(Constants.LayoutMap.get(name),name,false));
            file.getFirstChild().replace(tag);
        }else {
            file.getFirstChild().replace(getLayoutTag(Constants.LayoutMap.get(name),name,true));
        }
    }

    public void addActivityInManifest(String activityPackageName,String activityName){
        VirtualFile manifestVirtualFile = LocalFileSystem.getInstance().findFileByPath(Utils.getAndroidManifestPath(mProject));
        if(manifestVirtualFile!=null){
            XmlFile manifestFile = (XmlFile)PsiManager.getInstance(mProject).findFile(manifestVirtualFile);
            XmlTag applicationTag = manifestFile.getRootTag().findFirstSubTag("application");
            StringBuilder sb = new StringBuilder("<activity\n   android:name=\"");
            sb.append(activityPackageName.substring(Utils.readAndroidPackage(mProject).length()))
                    .append(".")
                    .append(activityName)
                    .append("\"")
                    .append("\n android:screenOrientation=\"portrait\"/>");
            XmlTag activityTag = XmlElementFactory.getInstance(mProject).createTagFromText(sb.toString());
            applicationTag.addSubTag(activityTag,false);
        }
    }

    private XmlFile createXmlIfNotExist(GenerateParams params){
        String fileName = "";
        XmlFile layout = null;
        if(params.getClassName().endsWith("Activity")){
            fileName = Utils.splitClassByUnderline("activity",params.getClassName().substring(0,params.getClassName().length()-8));
        }else if(params.getClassName().endsWith("Fragment")){
            fileName = Utils.splitClassByUnderline("fragment",params.getClassName().substring(0,params.getClassName().length()-8));
        }
        VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(Utils.getAndroidLayoutPath(mProject));
        if(PsiManager.getInstance(mProject).findDirectory(directory)!=null){
            layout = (XmlFile)PsiManager.getInstance(mProject).findDirectory(directory).findFile(fileName+".xml");
            if(layout == null){
                layout = (XmlFile)PsiManager.getInstance(mProject).findDirectory(directory).createFile(fileName+".xml");
            }
        }
        return layout;
    }

    private void modifiedDataBindingTag(XmlTag tag){
//        tag.setName("layout");
        tag.setAttribute("android","xmlns","http://schemas.android.com/apk/res/android");
        tag.setAttribute("app","xmlns","http://schemas.android.com/apk/res-auto");

    }

    private void addLayoutTag(XmlTag tag,String name,boolean isRoot){
        tag.setName(name);
        if(isRoot){
            tag.setAttribute("android","xmlns","http://schemas.android.com/apk/res/android");
            tag.setAttribute("app","xmlns","http://schemas.android.com/apk/res-auto");
        }
        tag.setAttribute("layout_width","android","match_parent");
        tag.setAttribute("layout_height","android","match_parent");
    }

    private XmlTag getDataBindingTag(){
        return XmlElementFactory.getInstance(mProject).createTagFromText(getDataBindingText());
    }

    private XmlTag getLayoutTag(String packageName,String layoutName,boolean isRoot){
        return XmlElementFactory.getInstance(mProject).createTagFromText(getLayoutTagText(packageName, layoutName, isRoot));
    }



    private String getDataBindingText(){
        StringBuilder sb = new StringBuilder("<layout");
        sb.append("\n")
                .append(DEFAULT_ATTR)
                .append(">\n")
                .append("\n</layout>");
        return sb.toString();
    }

    private String getLayoutType(String packageName,String layoutName){
        String name = packageName;
        if(name!=null&&name.length()>0){
            name = name + "." + layoutName;
        }else {
            name = layoutName;
        }
        return name;
    }

    private String getLayoutTagText(String packageName,String layoutName,boolean isRoot){
        String name = getLayoutType(packageName, layoutName);
        StringBuilder sb = new StringBuilder("<");
        sb.append(name)
                .append("\n");
        if(isRoot){
            sb.append(DEFAULT_ATTR);
            sb.append("\n");
        }
        sb.append("android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"match_parent\"")
                .append(">\n")
                .append("\n</")
                .append(name)
                .append(">");
        return sb.toString();
    }
}
