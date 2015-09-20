package com.developerphil.adbidea.adb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkUtils;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.developerphil.adbidea.adb.command.receiver.GenericReceiver;
import com.developerphil.adbidea.ui.NotificationHelper;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;

public class AdbUtil {

	public static String connect = "connect 192.168.";

	public static boolean isAppInstalled(IDevice device, String packageName)
			throws TimeoutException, AdbCommandRejectedException,
			ShellCommandUnresponsiveException, IOException {
		GenericReceiver receiver = new GenericReceiver();
		// "pm list packages com.my.package" will return one line per package
		// installed that corresponds to this package.
		// if this list is empty, we know for sure that the app is not installed
		device.executeShellCommand("pm list packages " + packageName, receiver,
				5L, TimeUnit.MINUTES);

		// TODO make sure that it is the exact package name and not a subset.
		// e.g. if our app is called com.example but there is another app called
		// com.example.another.app, it will match and return a false positive
		return !receiver.getAdbOutputLines().isEmpty();
	}

	/**
	 * Computes the project's package while preserving backward compatibility
	 * between android studio 0.4.3 and 0.4.4
	 */
	public static String computePackageName(AndroidFacet facet) {
		try {
			Object androidModuleInfo = facet.getClass()
					.getMethod("getAndroidModuleInfo").invoke(facet);
			return (String) androidModuleInfo.getClass().getMethod("getPackage")
					.invoke(androidModuleInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @author dvdandroid
	 */
	public static void startAdbCommand(Project project, String command)
			throws Exception {
		String output = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(Runtime
					.getRuntime()
					.exec(String.valueOf(
							AndroidSdkUtils.getAdb(project) + " " + command))
					.getInputStream()));

			String strLine;
			while ((strLine = br.readLine()) != null) {
				output = output + strLine + "\n";
			}
			br.close();

			switch (command.toLowerCase()) {
				case "start-server":
					output = "Server started successfully";
					break;
				case "kill-server":
					output = "Server killed successfully";
					break;
			}

		} catch (Throwable e) {
			NotificationHelper.sendNotification("ERROR",
					NotificationType.ERROR);
		} finally {
			boolean error = command.equals("start-server")
					|| command.contains("connect")
							? output.contains("error")
									|| output.contains("unable")
							: output.contains("not") || output.contains("error")
									|| output.contains("unable");

			NotificationHelper.sendNotification(output, error
					? NotificationType.ERROR : NotificationType.INFORMATION);
		}

	}
}