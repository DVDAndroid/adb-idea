package com.dvd.intellijidea.adbidea.action;

import static com.developerphil.adbidea.adb.AdbUtil.connect;

import com.developerphil.adbidea.action.AdbAction;
import com.developerphil.adbidea.adb.AdbUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * Created by dvdandroid on 12/04/2015.
 */
public class ConnectAction102 extends AdbAction {
	@Override
	public void actionPerformed(AnActionEvent event, Project project) {
		try {
			AdbUtil.startAdbCommand(project, connect + "1.102");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}