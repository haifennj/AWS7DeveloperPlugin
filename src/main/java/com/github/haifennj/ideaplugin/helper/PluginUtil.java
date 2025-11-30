package com.github.haifennj.ideaplugin.helper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * Created by Haiifenng on 2017.01.16.
 */
public class PluginUtil {

	private static volatile boolean isAWS7 = false;

	public static Module getReleaseModule(Project project) {
		return getReleaseModule(project, false);
	}

	@Nullable
	public static Module getReleaseModule(Project project, boolean isMsg) {
		Module releaseModule = findReleaseModule(project);
		if (releaseModule == null) {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中没有命名为[release]的Module");
			}
			System.err.println("当前Project中没有命名为[release]的Module");
			return null;
		}
		if (!"aws.release".equals(releaseModule.getName()) && releaseModule.getModuleFile() == null) {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中的[release]的不是一个有效的AWS资源");
			}
			System.err.println("当前Project中的[release]的不是一个有效的AWS资源");
			return null;
		}
		VirtualFile file = findReleaseModuleFile(project);
		//校验是不是一个有效的release
		if (file != null) {
			return releaseModule;
		} else {
			if (isMsg) {
				NotificationUtil.showErrorNotification(project, "当前Project中的[release]的不是一个有效的AWS资源");
			}
			System.err.println("当前Project中的[release]的不是一个有效的AWS资源");
			return null;
		}
	}

	public static Module findReleaseModule(Project project) {
		Module releaseModule = null;
		Module[] allModules = ModuleManager.getInstance(project).getModules();
		for (Module module : allModules) {
			String moduleName = module.getName().toLowerCase();

			// 多种匹配条件
			if (moduleName.equals("release") ||
					moduleName.contains("release") ||
					moduleName.equals("aws.release")) {
				releaseModule = module;
				break;
			}
		}
		return releaseModule;
	}

	public static VirtualFile findReleaseModuleFile(Project project) {
		Module releaseModule = findReleaseModule(project);
		if (releaseModule == null) {
			return null;
		}
		ModuleRootManager rootManager = ModuleRootManager.getInstance(releaseModule);
		VirtualFile[] contentRoots = rootManager.getContentRoots();
		for (VirtualFile virtualFile : contentRoots) {
			if (virtualFile.isDirectory()) {
				boolean releaseDir = isReleaseDir(virtualFile);
				if (releaseDir) {
					return virtualFile;
				}
			}
		}
		return null;
	}

	public static List<File> findAllFileInPath(String rootPath, FilenameFilter filenameFilter) {
		List<File> result = new ArrayList<>();
		File rootFile = new File(rootPath);
		LinkedList<File> list = new LinkedList<>();
		File[] childs = rootFile.listFiles();
		if (childs != null) {
			for (File child : childs) {
				list.add(child);
			}

		}
		while (!list.isEmpty()) {
			File wrap = list.removeFirst();
			if ((!wrap.isDirectory()) && (filenameFilter.accept(wrap, wrap.getName()))) {
				result.add(wrap);
			}

			childs = wrap.listFiles();
			if (childs != null) {
				for (File child : childs) {
					list.add(child);
				}
			}
		}
		return result;
	}

	public static boolean isAvailablePlatformModule(Module module) {
		if (module == null) {
			return false;
		}
		String name = module.getName();
		if (PluginUtil.isExcludeModule(module)) {
			return false;
		}
//		if (name.startsWith("aws-")) {
//			return true;
//		} else {
//			String moduleFilePath = module.getModuleFilePath();
//			if (moduleFilePath.contains("apps/install") || moduleFilePath.contains("apps/")) {
//				return true;
//			} else {
//				return false;
//			}
//		}
		if (isAvailableAppModule(module)) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isAvailableAppModule(Module module) {
		if (module == null) {
			return false;
		}
		String name = module.getName();
		if (PluginUtil.isExcludeModule(module)) {
			return false;
		}
		String moduleFilePath = module.getModuleFilePath();
		if (checkManifestXml(module.getModuleFile()) && (moduleFilePath.contains("apps/install") || moduleFilePath.contains("apps/"))) {
			return true;
		} else {
			return false;
		}
	}

	//	public static boolean isExcludeModule(String name) {
	//		String[] excludes = { "docs", "release", "aws-all", "aws", "apps", "web", "h5designer", "aws-security", "security", "aws-schema" };
	//		List<String> strings = Arrays.asList(excludes);
	//		return strings.contains(name);
	//	}

	/**
	 * 是否排除的module
	 * @param module
	 * @return
	 */
	public static boolean isExcludeModule(Module module) {
		VirtualFile file = module.getModuleFile();
		if (file == null) {
			return false;
		}
		VirtualFile srcMainPath = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(file.getParent().getPath(), "src/main/java"));
		VirtualFile srcPath = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(file.getParent().getPath(), "src"));
		if (srcMainPath != null && srcMainPath.exists()) {
			return false;
		}
		if (srcPath != null && srcPath.exists()) {
			return false;
		}
		return true;
	}

	public static boolean isAWS7() {
		return isAWS7;
	}

	// 修改 checkAws7 方法，使其返回 Future<Boolean> 并正确处理 read-action
	public static Future<Boolean> checkAws7(Project project) {
		return ApplicationManager.getApplication().executeOnPooledThread(() ->
			ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
				boolean result = realCheckAws7(project);
				isAWS7 = result; // 更新静态变量
				return result;
			})
		);
	}

	// 其他方法保持不变

	private static boolean realCheckAws7(Project project) {
		Collection<VirtualFile> virtualFilesByName = FilenameIndex.getVirtualFilesByName(project, "release", GlobalSearchScope.allScope(project));
		for (VirtualFile virtualFile : virtualFilesByName) {
			if (virtualFile.isDirectory()) {
				String releasePath = virtualFile.getPath();
				File file_release7_1 = new File(releasePath + "/bin/conf/application-dev.yml");
				File file_release7_2 = new File(releasePath + "/bin/conf/bootstrap.yml");
				if (file_release7_1.exists() && file_release7_2.exists()) {//AWS7版本
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isReleaseDir(VirtualFile file) {
		//校验是不是一个有效的release
		String releasePath = file.getPath();
		File file_release7_1 = new File(releasePath + "/bin/conf/application-dev.yml");
		File file_release7_2 = new File(releasePath + "/bin/conf/bootstrap.yml");

		File file_release6_1 = new File(releasePath + "/bin/conf/server.xml");
		File file_release6_2 = new File(releasePath + "/bin/lib/aws-license.jar");

		File file_release5_1 = new File(releasePath + "/bin/system.xml");
		File file_release5_2 = new File(releasePath + "/bin/lib/aws.platform.jar");

		if (file_release7_1.exists() && file_release7_2.exists()) {//AWS7版本
			return true;
		} else if (file_release6_1.exists() && file_release6_2.exists()) {//AWS6版本
			return true;
		} else if (file_release5_1.exists() && file_release5_2.exists()) {//AWS5版本
			return true;
		} else {
			return false;
		}
	}

	public static boolean isJavaModuleDir(VirtualFile file) {
		if (file == null) {
			return false;
		}
		VirtualFile srcMainPath = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(file.getPath(), "src/main/java"));
		VirtualFile srcPath = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(file.getPath(), "src"));
		if (srcMainPath != null && srcMainPath.exists()) {
			return true;
		}
		if (srcPath != null && srcPath.exists()) {
			return true;
		}
		return false;
	}

	public static boolean isAWSWebModule(String name) {
		String[] excludes = { "aws-infrastructure-web", "aws-node-wrapper", "aws-coe-web", "aws-api-client" };
		List<String> strings = Arrays.asList(excludes);
		return strings.contains(name);
	}

	public static List<String> getAppDirs(File installDir) {
		List<String> list = new ArrayList<>();
		File[] files = installDir.listFiles();
		for (File file : files) {
			if (".DS_Store".equals(file.getName())) {
				continue;
			}
			if (file.isDirectory() && !file.getName().startsWith("_bpm")) {
				list.add(file.getName());
			}
		}
		return list;
	}

	public static boolean checkManifestXml(VirtualFile file) {
		if (file == null) {
			return false;
		}
		File manifestFile = null;
		if (file.isDirectory()) {
			manifestFile = new File(file.getPath() + "/manifest.xml");
		} else {
			manifestFile = new File(file.getParent().getPath() + "/manifest.xml");
		}
		return manifestFile.exists();
	}

}
