package com.dvd.intellijidea.adbidea.action;

import com.developerphil.adbidea.action.AdbAction;
import com.developerphil.adbidea.adb.AdbUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * Created by dvdandroid on 12/04/2015.
 */
public class StartServerAction extends AdbAction {

	@Override
	public void actionPerformed(AnActionEvent event, Project project) {
		try {
			AdbUtil.startAdbCommand(project, "start-server");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}