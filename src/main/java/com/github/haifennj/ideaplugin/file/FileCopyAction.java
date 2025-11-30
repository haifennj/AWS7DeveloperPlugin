package com.github.haifennj.ideaplugin.file;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;

public class FileCopyAction extends AnAction {

	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		// 因为我们要访问 VirtualFile，可能在后台线程中执行更安全
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(AnActionEvent anActionEvent) {
		VirtualFile[] data = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(anActionEvent.getDataContext());
		for (VirtualFile file : data) {
			FileCopy fc = new FileCopy(anActionEvent.getProject());
			fc.copyToDesktop(file);
		}
	}

	@Override
	public void update(AnActionEvent e) {
		if (e.getProject() == null) return;
		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

		ApplicationManager.getApplication().invokeLater(() -> {
			if (file != null) {
				boolean isValid = file.isValid();
				e.getPresentation().setEnabled(isValid);
			}
		});
	}
}
