package com.developerphil.adbidea.action;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.ide.actions.QuickSwitchSchemeAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public class QuickListAction extends QuickSwitchSchemeAction
		implements DumbAware {

	protected void fillActions(@Nullable final Project project,
			@NotNull final DefaultActionGroup group,
			@NotNull final DataContext dataContext) {

		if (project == null) {
			return;
		}

		addAction("com.developerphil.adbidea.action.UninstallAction", group);
		addAction("com.developerphil.adbidea.action.KillAction", group);
		addAction("com.developerphil.adbidea.action.StartAction", group);
		addAction("com.developerphil.adbidea.action.RestartAction", group);
		addAction("com.developerphil.adbidea.action.ClearDataAction", group);
		addAction("com.developerphil.adbidea.action.ClearDataAndRestartAction",
				group);
	}

	protected boolean isEnabled() {
		return true;
	}

	private void addAction(final String actionId,
			final DefaultActionGroup toGroup) {
		final AnAction action = ActionManager.getInstance().getAction(actionId);

		// add action to group if it is available
		if (action != null) {
			toGroup.add(action);
		}
	}

	protected String getPopupTitle(AnActionEvent e) {
		return "ADB Operations Popup";
	}

}
