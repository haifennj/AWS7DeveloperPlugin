package com.github.haifennj.ideaplugin.gradle;

import java.util.Collections;
import java.util.List;

import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

public class RunGradleTaskAction extends AnAction {

    public RunGradleTaskAction() {
        super("Run Gradle Task");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        // Project project = e.getProject();
        // if (project == null) return;
		//
        // Module module = e.getData(LangDataKeys.MODULE);
        // if (module == null) return;
		//
        // String modulePath = module.getModuleFile() != null ? module.getModuleFile().getParent().getPath() : null;
        // // if (modulePath == null) return;
		//
        // // 显示任务选择 UI
        // GradleTaskPopup.showTaskSelectionPopup(project, selectedTask -> {
        //     runGradleTask(project, null, selectedTask);
        // });

		Module module = LangDataKeys.MODULE.getData(e.getDataContext());
		if (module == null) return;

		Project project = module.getProject();
		String taskName = "build"; // 可以自定义或弹出选择框

		String projectPath = ExternalSystemApiUtil.getExternalProjectPath(module);
		if (projectPath == null) return;

		ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
		settings.setExternalProjectPath(projectPath);
		settings.setTaskNames(Collections.singletonList(taskName));
		settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());

		ExternalSystemUtil.runTask(
				settings,
				DefaultRunExecutor.EXECUTOR_ID,
				project,
				GradleConstants.SYSTEM_ID,
				null, // callback
				ProgressExecutionMode.IN_BACKGROUND_ASYNC
		);
    }

    private void runGradleTask(Project project, String modulePath, String task) {
		GradleExternalTaskConfigurationType type = GradleExternalTaskConfigurationType.getInstance();
        GradleRunConfiguration runConfiguration = (GradleRunConfiguration) type.getFactory().createTemplateConfiguration(project);

        runConfiguration.getSettings().setExternalProjectPath(modulePath);
        runConfiguration.getSettings().setTaskNames(List.of(task));

        Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        ProgramRunner<?> runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, runConfiguration);
        if (runner == null) return;

        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(executor, runConfiguration).build();
		try {
			runner.execute(environment);
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
