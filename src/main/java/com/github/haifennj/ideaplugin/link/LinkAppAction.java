package com.github.haifennj.ideaplugin.link;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import com.github.haifennj.ideaplugin.gradle.GenerateGradle;
import com.github.haifennj.ideaplugin.helper.NotificationUtil;
import com.github.haifennj.ideaplugin.helper.OSUtil;
import com.github.haifennj.ideaplugin.helper.PluginUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Created by Haiifenng on 2017.05.19.
 */
public class LinkAppAction extends AnAction {
	@Override
	public @NotNull ActionUpdateThread getActionUpdateThread() {
		// 因为我们要访问 VirtualFile，可能在后台线程中执行更安全
		return ActionUpdateThread.BGT;
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		VirtualFile[] data = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
		if (data != null) {
			Module releaseModule = PluginUtil.getReleaseModule(e.getProject(), true);
			if (releaseModule == null) {
				return;
			}
			boolean hasCreateOperation = false;
			boolean hasRemoveOperation = false;
			for (VirtualFile file : data) {
				if (checkFileExist(e, file)) {
					removeLink(e.getProject(), file);
					hasRemoveOperation = true;
				} else {
					createLink(e.getProject(), file);
					hasCreateOperation = true;
				}
			}
			// 根据操作类型分别刷新和提示
			if (hasCreateOperation || hasRemoveOperation) {
				refreshVirtualFile(e.getProject());

				// 根据具体操作组合提示信息
				if (hasCreateOperation && hasRemoveOperation) {
					NotificationUtil.showInfoNotification(e, "符号链接更新成功 - 已删除旧链接并创建新链接");
				} else if (hasRemoveOperation) {
					NotificationUtil.showInfoNotification(e, "符号链接删除成功");
				} else if (hasCreateOperation) {
					NotificationUtil.showInfoNotification(e, "符号链接创建成功");
				}
			}
		}
	}

	/**
	 * 刷新虚拟文件
	 */
	private void refreshVirtualFile(Project project) {
		String installPath = getReleasePath(project) + "/apps/install/";
		VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(installPath));
		if (virtualFile != null) {
			virtualFile.refresh(false, true);
		}
	}

	private static @NonNls @NotNull String getReleasePath(Project project) {
		VirtualFile releaseModuleFile = PluginUtil.findReleaseModuleFile(project);
		return releaseModuleFile != null ? releaseModuleFile.getPath() : "";
	}

	protected void createLink(Project project, VirtualFile file) {
		List<Map<String, String>> list = new ArrayList<>();
		String targetFile = getReleasePath(project) + "/apps/install/" + file.getName();
		String sourceFile = file.getPath();
		Map<String, String> cmdPath = null;
		// 检查sourceFile下面有没有web目录
		String webRoot = "/webserver/webapps/portal";
		String webTargetFile = getReleasePath(project) + webRoot + "/apps/" + file.getName();
		String webSourceFile = file.getPath() + "/web/" + file.getName();
		File webSourceFileObj = new File(webSourceFile);
		if (webSourceFileObj.exists()) {
			// cd /data/develop/release/webserver/webapps/portal/apps/
			// ln -s /data/develop/release/apps/install/com.actionsoft.apps.dingding.yijing/web/com.actionsoft.apps.dingding.yijing com.actionsoft.apps.dingding.yijing
			cmdPath = new HashMap<>();
			cmdPath.put("targetFile", webTargetFile);
			cmdPath.put("sourceFile", webSourceFile);
			list.add(cmdPath);
		}
		cmdPath = new HashMap<>();
		cmdPath.put("targetFile", targetFile);
		cmdPath.put("sourceFile", sourceFile);
		list.add(cmdPath);
		for (Map<String, String> map : list) {
			execCMD(map.get("targetFile"), map.get("sourceFile"));
		}
	}
	protected void removeLink(Project project, VirtualFile file) {
		String releasePath = getReleasePath(project);
		String targetFile = releasePath + "/apps/install/" + file.getName();
		// 检查sourceFile下面有没有web目录
		String webRoot = "/webserver/webapps/portal";
		String webTargetFile = releasePath + webRoot + "/apps/" + file.getName();

		// 删除 targetFile
		deleteFileOrLink(targetFile);

		// 删除 webTargetFile
		deleteFileOrLink(webTargetFile);
	}

	private void deleteFileOrLink(String filePath) {
		File file = new File(filePath);

		if (!file.exists()) {
			System.out.println("文件不存在: " + filePath);
			return;
		}

		try {
			// 检查是否为软链接
			if (Files.isSymbolicLink(file.toPath())) {
				// 删除软链接
				Files.delete(file.toPath());
				System.out.println("已删除软链接: " + filePath);
			} else {
				// 删除物理文件或目录
				if (file.isDirectory()) {
					deleteDirectory(file);
					System.out.println("已删除目录: " + filePath);
				} else {
					Files.delete(file.toPath());
					System.out.println("已删除文件: " + filePath);
				}
			}
		} catch (IOException e) {
			System.err.println("删除失败: " + filePath + " - " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 递归删除目录
	 */
	private void deleteDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					deleteDirectory(file);
				}
			}
		}
		Files.delete(dir.toPath());
	}

	private void execCMD(String targetFile, String sourceFile) {
		String cmd = "";
		if (OSUtil.isMacOSX() || OSUtil.isLinux()) {
			cmd = "ln -s " + sourceFile + " " + targetFile;
			link(cmd);
		} else if (OSUtil.isWindows()) {
			sourceFile = sourceFile.replaceAll("/", "\\\\");
			targetFile = targetFile.replaceAll("/", "\\\\");
			cmd = "cmd.exe /c mklink /j " + targetFile + " " + sourceFile;
			link(cmd);
		}
	}

	protected void link(String cmd) {
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(cmd);
			process.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void update(AnActionEvent e) {
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
		String filePath = file.getPath();
		String flag = "/apps/";
		if (PluginUtil.getReleaseModule(e.getProject()) == null) {
			e.getPresentation().setVisible(false);
			return;
		}
		if (!file.isDirectory()) {
			e.getPresentation().setVisible(false);
			return;
		}
		if (filePath.contains("/apps/install/") || filePath.contains("release/")) {
			e.getPresentation().setVisible(false);
			return;
		}
		if (!isMulti) {
			int existType = checkFileExistType(e, file);
			if (existType == FILE_IS_LINK) {
				e.getPresentation().setText("Link App (Already linked - Delete)");
				return;
			} else if (existType == FILE_EXIST) {
				e.getPresentation().setText("Link App (Real files - Delete)");
				return;
			} else {
				GenerateGradle generateGradle = new GenerateGradle();
				if (generateGradle.checkFileExist(e, file)) {
					e.getPresentation().setText("Link App (+Gradle, needs refresh)");
					e.getPresentation().setEnabled(false);
					return;
				}
			}
		}
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

	public static final int FILE_NOT_EXIST = 0;
	public static final int FILE_EXIST = 1;
	public static final int FILE_IS_LINK = 2;

	protected boolean checkFileExist(AnActionEvent e, VirtualFile file) {
		int i = checkFileExistType(e, file);
		return i == FILE_EXIST || i == FILE_IS_LINK;
	}
	protected int checkFileExistType(AnActionEvent e, VirtualFile file) {
		VirtualFile releaseModuleFile = PluginUtil.findReleaseModuleFile(e.getProject());
		if (releaseModuleFile != null) {
			String targetFilePath = releaseModuleFile.getPath() + "/apps/install/" + file.getName();
			if (new File(targetFilePath).exists()){
				Path path = Paths.get(targetFilePath);
				if (Files.isSymbolicLink(path)) {
					return FILE_IS_LINK;
				} else {
					return FILE_EXIST;
				}
			}
		}
		return FILE_NOT_EXIST;
	}

}
