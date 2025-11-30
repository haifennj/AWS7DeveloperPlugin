package com.github.haifennj.ideaplugin.gradle;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.github.haifennj.ideaplugin.helper.PluginUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

public class GenerateGradle extends AnAction {
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		// 因为我们要访问 VirtualFile，可能在后台线程中执行更安全
		return ActionUpdateThread.BGT;
	}
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 获取当前项目和选中的文件
        Project project = e.getProject();
        VirtualFile[] data = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        VirtualFile localGradleFile = getLocalGradleFile(project);

        if (data != null && data.length > 0) {
            if (localGradleFile != null) {
                boolean isExist = false;
                // 生成代码并写入local.gradle
                StringBuilder codeBuilder = new StringBuilder();
                for (VirtualFile file : data) {
                    isExist = checkFileExist(e, file);
                    codeBuilder.append(getCode(project, file));
                }

                // 将代码写入local.gradle文件
                boolean finalIsExist = isExist;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        Document document = FileDocumentManager.getInstance().getDocument(localGradleFile);
                        if (document != null) {
                            if (finalIsExist) {
                                deleteLinesWithText(document, codeBuilder.toString());
                            } else {
                                document.insertString(document.getTextLength(), codeBuilder.toString()
                                );
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        }
    }

    private void deleteLinesWithText(Document document, String textToDelete) {
        String documentText = document.getText();
        int start = documentText.indexOf(textToDelete);
        int end = start+textToDelete.length();
        document.deleteString(start, end);
    }

    private VirtualFile getLocalGradleFile(Project project) {
        // 使用 FilenameIndex 来查找 local.gradle 文件
        Collection<VirtualFile> localGradleFiles = FilenameIndex.getVirtualFilesByName(project, "local.gradle", GlobalSearchScope.allScope(project));

        for (VirtualFile file : localGradleFiles) {
            // 处理找到的 local.gradle 文件，比如打开或进行其他操作
            if ("local.gradle".equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    private String getCode(Project project, VirtualFile file) {
        if (project == null) {
            return "";
        }
        String path = file.getPath();
        String projectPath = project.getBasePath() == null ? "" : project.getBasePath();
        if (path.contains("/apps/")) {
            path = "../.." + path.replace(projectPath,"");
        }
        String fileName = file.getName();
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("// ").append(fileName).append("\n");
        codeBuilder.append("include('").append(fileName).append("')\n");
        codeBuilder.append("project(':").append(fileName).append("').projectDir = new File(settingsDir,'").append(path).append("')\n");
        return codeBuilder.toString();
    }


    @Override
    public void update(AnActionEvent e) {
        VirtualFile[] data = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        if (file == null) {
            e.getPresentation().setVisible(false);
        } else {
            if ((data != null ? data.length : 0) > 1) {
                e.getPresentation().setVisible(false);
            } else {
                checkFile(e, file, false);
            }
        }
    }

    private void checkFile(AnActionEvent e, VirtualFile file, boolean isMulti) {
        String flag = "/apps/";
        if (!file.isDirectory()) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (file.getPath().contains("/apps/install/") || file.getPath().contains("release/")) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (!isMulti && checkFileExist(e, file)) {
            e.getPresentation().setText("Remove from 'local.gradle'");
            e.getPresentation().setEnabled(true);
            return;
        }
        String filePath = file.getPath();
        if (file.getName().startsWith("_bpm")) {
            e.getPresentation().setVisible(false);
            return;
        }
        if (file.getName().startsWith("_platform")) {
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

    public boolean checkFileExist(AnActionEvent e, VirtualFile file) {
        VirtualFile localGradleFile = getLocalGradleFile(e.getProject());
        if (localGradleFile == null) {
            return false;
        }
        String fileCode = getCode(e.getProject(), file);
        String text = Objects.requireNonNull(FileDocumentManager.getInstance().getDocument(localGradleFile)).getText();
        return text.contains(fileCode);
    }

}
