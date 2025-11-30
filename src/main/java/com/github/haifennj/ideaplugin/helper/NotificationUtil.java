package com.github.haifennj.ideaplugin.helper;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

public class NotificationUtil {
	// 获取通知组管理器
	// private static NotificationGroupManager manager = NotificationGroupManager.getInstance();
	// 获取注册的通知组
	// private static NotificationGroup balloon = manager.getNotificationGroup("com.github.haifennj.ideaplugin.notification.balloon");

	// private static NotificationGroup getNotificationGroup() {
	// 	try {
	// 		// 高版本 (2020.1+)
	// 		return (NotificationGroup) NotificationGroupManager.class
	// 				.getMethod("getNotificationGroup", String.class)
	// 				.invoke(NotificationGroupManager.getInstance(), "com.github.haifennj.ideaplugin.notification.balloon");
	// 	} catch (Exception e) {
	// 		// 低版本 (2019.3 及以下)
	// 		return new NotificationGroup(
	// 				"com.github.haifennj.ideaplugin.notification.balloon",
	// 				NotificationDisplayType.BALLOON,
	// 				true
	// 		);
	// 	}
	// }

	private static void showNotification(AnActionEvent anActionEvent, String content, NotificationType notificationType) {
		ApplicationManager.getApplication().invokeLater(() -> {
			Notification notification = new Notification("AWS Developer Plugins Notification Group", "AWS Developer Plugins", content, notificationType);
			// notification.notify(AnAction.getEventProject(anActionEvent));
			Notifications.Bus.notify(notification);
		});
	}

	public static void showInfoNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.INFORMATION);
	}

	public static void showWarningNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.WARNING);
	}

	public static void showErrorNotification(AnActionEvent anActionEvent, String content) {
		showNotification(anActionEvent, content, NotificationType.ERROR);
	}

	private static void showNotification(String content, NotificationType notificationType) {
		ApplicationManager.getApplication().invokeLater(() -> {
			Notification notification = new Notification("AWS Developer Plugins Notification Group", "AWS Developer Plugins", content, notificationType);
			Notifications.Bus.notify(notification);
		});
	}

	public static void showInfoNotification(Project project, String content) {
		showNotification(content, NotificationType.INFORMATION);
	}

	public static void showWarningNotification(Project project, String content) {
		showNotification(content, NotificationType.WARNING);
	}

	public static void showErrorNotification(Project project, String content) {
		showNotification(content, NotificationType.ERROR);
	}

	public static void info(String msg) {
		showNotification(null, msg, NotificationType.INFORMATION);
		// Notifications.Bus.notify(notification);
	}

	public static void warning(String msg) {
		showNotification(null, msg, NotificationType.WARNING);
		// Notifications.Bus.notify(notification);
	}

	public static void error(String msg) {
		showNotification(null, msg, NotificationType.ERROR);
		// Notifications.Bus.notify(notification);
	}
}
