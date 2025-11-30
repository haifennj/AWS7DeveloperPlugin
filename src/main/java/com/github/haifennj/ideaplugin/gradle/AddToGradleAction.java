package com.github.haifennj.ideaplugin.gradle;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import com.github.haifennj.ideaplugin.helper.PluginUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;

public class AddToGradleAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
//         ModuleManager moduleManager = ModuleManager.getInstance(anActionEvent.getProject());
// //        for (Module module : moduleManager.getModules()) {
// //            System.out.println(module);
// //        }
//         Module aws = ModuleManager.getInstance(anActionEvent.getProject()).findModuleByName("aws");
//         System.err.println(aws);
//
//        ProjectConnection projectConnection = GradleConnector.newConnector().connect();
//        GradleProject rootProject = projectConnection.getModel(GradleProject.class);

    }

    @Override
    public void update(AnActionEvent e) {
//        e.getPresentation().setVisible(true);
        VirtualFile[] data = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (file == null) {
            e.getPresentation().setVisible(false);
        } else {
            if (data.length > 1) {
                for (VirtualFile virtualFile : data) {
                    checkFile(e, virtualFile, true);
                }
                e.getPresentation().setText("Link Apps");
            } else {
                checkFile(e, file, false);
            }
        }
    }

    private void checkFile(AnActionEvent e, VirtualFile file, boolean isMulti) {
        String flag = "/apps/";
        if (PluginUtil.getReleaseModule(e.getProject()) == null) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (!file.isDirectory()) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (file.getPath().contains("/apps/install/") || file.getPath().contains("release/")) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (!isMulti) {
            e.getPresentation().setText("Already Linked");
            e.getPresentation().setEnabled(false);
            return;
        }
        String filePath = file.getPath();
        if (file.getName().startsWith("_bpm")) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (!checkManifestXml(e, file)) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (filePath.contains(flag)) {
            String appId = filePath.substring(filePath.indexOf(flag) + flag.length());
            //说明是子文件夹或文件
            if (appId.contains("/")) {
                if (StringUtil.countChars(appId, File.separatorChar) != 1) {
                    e.getPresentation().setVisible(false);
                }
            }
        } else {
            e.getPresentation().setVisible(false);
        }
    }

    protected boolean checkManifestXml(AnActionEvent e, VirtualFile file) {
        return PluginUtil.checkManifestXml(file);
    }

}
