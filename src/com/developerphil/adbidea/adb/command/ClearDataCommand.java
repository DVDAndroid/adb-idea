package com.developerphil.adbidea.adb.command;

import static com.developerphil.adbidea.adb.AdbUtil.isAppInstalled;
import static com.developerphil.adbidea.ui.NotificationHelper.error;
import static com.developerphil.adbidea.ui.NotificationHelper.info;

import java.util.concurrent.TimeUnit;

import org.jetbrains.android.facet.AndroidFacet;

import com.android.ddmlib.IDevice;
import com.developerphil.adbidea.adb.command.receiver.GenericReceiver;
import com.intellij.openapi.project.Project;

public class ClearDataCommand implements Command {

	@Override
	public boolean run(Project project, IDevice device, AndroidFacet facet,
			String packageName) {
		try {
			if (isAppInstalled(device, packageName)) {
				device.executeShellCommand("pm clear " + packageName,
						new GenericReceiver(), 15L, TimeUnit.SECONDS);
				info(String.format("<b>%s</b> cleared data for app on %s",
						packageName, device.getName()));
				return true;
			} else {
				error(String.format("<b>%s</b> is not installed on %s",
						packageName, device.getName()));
			}
		} catch (Exception e1) {
			error("Clear data failed... " + e1.getMessage());
		}

		return false;
	}

}
