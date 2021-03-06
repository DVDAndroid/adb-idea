package com.developerphil.adbidea.adb.command;

import static com.developerphil.adbidea.ui.NotificationHelper.error;
import static com.developerphil.adbidea.ui.NotificationHelper.info;

import java.util.concurrent.TimeUnit;

import org.jetbrains.android.facet.AndroidFacet;

import com.android.ddmlib.IDevice;
import com.developerphil.adbidea.adb.command.receiver.GenericReceiver;
import com.intellij.openapi.project.Project;

public class ToggleDisplayCommand implements Command {

	@Override
	public boolean run(Project project, IDevice device, AndroidFacet facet,
			String packageName) {
		try {
			device.executeShellCommand("input keyevent 26",
					new GenericReceiver(), 5L, TimeUnit.MINUTES);
			info(String.format("Toggle the display state on %s",
					device.getName()));
			return true;

		} catch (Exception e1) {
			error("Toggle display failed" + e1.getMessage());
		}

		return false;
	}

}
