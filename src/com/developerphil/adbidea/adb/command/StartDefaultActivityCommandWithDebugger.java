package com.developerphil.adbidea.adb.command;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.tools.idea.ddms.adb.AdbService;
import com.developerphil.adbidea.adb.command.receiver.GenericReceiver;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.Uninterruptibles;
import com.intellij.execution.*;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.ui.content.Content;
import com.intellij.util.NotNullFunction;
import org.jetbrains.android.dom.AndroidDomUtil;
import org.jetbrains.android.dom.manifest.*;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.developerphil.adbidea.ui.NotificationHelper.error;
import static com.developerphil.adbidea.ui.NotificationHelper.info;

public class StartDefaultActivityCommandWithDebugger implements Command {
    public static final String LAUNCH_ACTION_NAME = "android.intent.action.MAIN";
    public static final String LAUNCH_CATEGORY_NAME = "android.intent.category.LAUNCHER";
    private static RunnerAndConfigurationSettings prvSettings;
    private static Project prvProject;
    private static IDevice prvDevice;
    private static RunContentDescriptor prvDescriptor;
    private static String prvPackageName;
    private static Executor prvExecutor;
    private static boolean debuggingStatus;

    @Override
    public boolean run(final Project project, final IDevice device, final AndroidFacet facet, final String packageName) {
        String defaultActivityName = getDefaultActivityName(facet);
        String component = packageName + "/" + defaultActivityName;

        if (prvProject == null) prvProject = project;
        if (prvDevice == null) prvDevice = device;
        if (prvPackageName == null) prvPackageName = packageName;

        try {
            StartActivityReceiver receiver = new StartActivityReceiver();
            device.executeShellCommand("am start -D -n " + component, receiver, 5L, TimeUnit.MINUTES);

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (!AndroidSdkUtils.activateDdmsIfNecessary(project)) {
                        error("activateDdmsIfNecessary returns false, unable to start debugging.");
                        debuggingStatus = false;
                    } else {
                        debuggingStatus = startDebugging(prvDevice, prvProject, prvPackageName);
                        if (!debuggingStatus) {
                            error("startDebugging returns false.");
                            info(String.format("<b>%s</b> forced-stop on %s", packageName, device.getName()));
                            try {
                                device.executeShellCommand("am force-stop " + packageName, new GenericReceiver(), 5L, TimeUnit.MINUTES);
                            } catch (Exception e) {
                                error("Force-stop failed... " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });


            if (receiver.isSuccess()) {
                if (debuggingStatus) {
                    info(String.format("<b>%s</b> started on %s", packageName, device.getName()));
                    return true;
                }
            } else {
                error(String.format("<b>%s</b> could not bet started on %s. \n\n<b>ADB Output:</b> \n%s", packageName, device.getName(), receiver.getMessage()));
            }
        } catch (Exception e) {
            error("Start fail... " + e.getMessage());
        }

        return false;
    }

    private String getDefaultActivityName(final AndroidFacet facet) {
        return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
                return getDefaultLauncherActivityName(facet.getManifest());
            }
        });
    }

    public static class StartActivityReceiver extends MultiLineReceiver {

        public String message = "Nothing Received";
        public final List<String> currentLines = new ArrayList<>();

        @Override
        public void processNewLines(String[] strings) {
            for (String s : strings) {
                if (!Strings.isNullOrEmpty(s)) {
                    currentLines.add(s);
                }
            }
            computeMessage();
        }

        private void computeMessage() {
            message = Joiner.on("\n").join(currentLines);
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return currentLines.size() > 0 && currentLines.size() < 3;
        }
    }

    @Nullable
    public static String getDefaultLauncherActivityName(@NotNull Manifest manifest) {
        Application application = manifest.getApplication();
        if (application == null) {
            return null;
        }

        for (Activity activity : application.getActivities()) {
            for (IntentFilter filter : activity.getIntentFilters()) {
                if (AndroidDomUtil.containsAction(filter, LAUNCH_ACTION_NAME) && AndroidDomUtil.containsCategory(filter, LAUNCH_CATEGORY_NAME)) {
                    PsiClass c = activity.getActivityClass().getValue();
                    return c != null ? c.getQualifiedName() : null;
                }
            }
        }

        for (ActivityAlias alias : application.getActivityAliass()) {
            for (IntentFilter filter : alias.getIntentFilters()) {
                if (AndroidDomUtil.containsAction(filter, LAUNCH_ACTION_NAME) && AndroidDomUtil.containsCategory(filter, LAUNCH_CATEGORY_NAME)) {
                    return alias.getName().getStringValue();
                }
            }
        }

        return null;
    }


    private void runDebugger(final String port, final Project project) {
        final String configurationName = String.format("Android Debugger (%s)", port);
        ProcessHandler processHandler;
        Content content;
        Collection descriptors;

        if (prvProject == null) prvProject = project;

        /* Attempt to close previous instance if one exists */
        descriptors = ExecutionHelper.findRunningConsoleByTitle(prvProject, new NotNullFunction<String, Boolean>() {
            @NotNull
            @Override
            public Boolean fun(String title) {
                return configurationName.equals(title);
            }
        });

        int size = descriptors.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                prvDescriptor = (RunContentDescriptor)descriptors.iterator().next();
                processHandler = prvDescriptor.getProcessHandler();
                content = prvDescriptor.getAttachedContent();

                if ((processHandler != null) && (content != null)) {
                    prvExecutor = DefaultDebugExecutor.getDebugExecutorInstance();
                    if (processHandler.isProcessTerminated()) {
                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                            public void run() {
                                ExecutionManager.getInstance(prvProject).getContentManager().removeRunContent(prvExecutor, prvDescriptor);
                            }
                        });
                    }
                }
            }
        }

        /* Start a new instance */
        RemoteConfigurationType remoteConfigurationType = RemoteConfigurationType.getInstance();

        ConfigurationFactory factory = remoteConfigurationType.getFactory();
        prvSettings = RunManager.getInstance(project).createRunConfiguration(configurationName, factory);

        RemoteConfiguration configuration = (RemoteConfiguration)prvSettings.getConfiguration();
        configuration.HOST = "localhost";
        configuration.PORT = port;
        configuration.USE_SOCKET_TRANSPORT = true;
        configuration.SERVER_MODE = false;

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
                ProgramRunnerUtil.executeConfiguration(prvProject, prvSettings, executor);
            }
        });
    }


    private boolean startDebugging(final IDevice device, final Project project, final String packageName) {
        Client clients[];
        Client client = null;
        if (device == null) {
            throw new IllegalArgumentException(
                    "ERROR: startDebugging(): device == null");
        }
        info(String.format("Target device: " + device.getName(), ProcessOutputTypes.STDOUT));

        try {
            AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
            if (bridge == null) {
                error("bridge == null, can't start debugger");
                return false;
            }

            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            IDevice [] devices = bridge.getDevices();
            for (IDevice device1 : devices) {
                if (device1 != null) {
                    client = device1.getClient(packageName);
                }
                if (client != null) break;
            }
            boolean canDdmsBeCorrupted = AdbService.canDdmsBeCorrupted(bridge);
            if (canDdmsBeCorrupted) {
                error("ERROR: ddms can be corrupted, can't start debugger.");
                return false;
            }

            if (client == null) {
                info("Trying to find client for device.");
                if (device.hasClients()) {
                    client = null;
                    clients = device.getClients();
                    for (Client client1 : clients) {
                        if (packageName.equalsIgnoreCase(client1.getClientData().getClientDescription())) {
                            client = client1;
                            break;
                        }
                    }
                    if (client == null) {
                        client = device.getClient(packageName);
                    }
                } else{
                    client = device.getClient(packageName);
                }
            }

            if (client == null) {
                error("ERROR: Can't start debugger.");
                return false;
            }

            final String port = Integer.toString(client.getDebuggerListenPort());
            runDebugger(port, project);

            return true;
        } catch (Exception e) {
            error("ERROR: Fatal Exception, unable to start debugger!");
        }

        return false;
    }
}

