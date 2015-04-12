package com.dvd.intellijidea.adbidea.action;

import com.developerphil.adbidea.action.AdbAction;
import com.developerphil.adbidea.adb.AdbUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * Created by dvdandroid on 12/04/2015.
 */
public class ConnectCustomIpAction extends AdbAction {
	@Override
	public void actionPerformed(AnActionEvent event, Project project) {
		String txt = Messages.showInputDialog(project,
				"Please insert your custom IP address", "Input Your Custom IP",
				Messages.getQuestionIcon());
		try {
			AdbUtil.startAdbCommand(project, " connect " + txt);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}