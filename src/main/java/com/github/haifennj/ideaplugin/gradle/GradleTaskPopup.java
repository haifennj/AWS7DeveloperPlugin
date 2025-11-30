package com.github.haifennj.ideaplugin.gradle;

import java.util.ArrayList;
import java.util.List;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.popup.list.ListPopupImpl;

public class GradleTaskPopup {
    public static void showTaskSelectionPopup(Project project, TaskSelectionCallback callback) {
        List<String> tasks = new ArrayList<>();//GradleTaskFetcher.getGradleTasks(modulePath);
        if (tasks.isEmpty()) {
            Messages.showMessageDialog(project, "No Gradle tasks found.", "Error", Messages.getErrorIcon());
            return;
        }

        ListPopupImpl popup = (ListPopupImpl) JBPopupFactory.getInstance()
                .createPopupChooserBuilder(tasks)
                .setTitle("Select a Gradle Task")
                .setItemChosenCallback(callback::onTaskSelected)
                .createPopup();

        popup.showInFocusCenter();
    }

    public interface TaskSelectionCallback {
        void onTaskSelected(String task);
    }
}
