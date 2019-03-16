package SkeletonGenerator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.python.sdk.InvalidSdkException;
import org.jetbrains.annotations.NotNull;
import com.jetbrains.python.sdk.skeletons.PySkeletonRefresher;
import com.jetbrains.python.sdk.PythonSdkType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.util.List;
import java.util.Vector;

public class SkeletonRefresher extends AnAction {
    private static String m_path_for_skeleton_to_copy = "C:\\git\\PythonDdsWrapper";
    static Boolean isGenerating = false;
    static final Object SYNC_OBJECT = new Object();
    private final double INDICATOR_PROGRESSION_AMOUNT = 0.3333;

    public SkeletonRefresher() {
        super("SkeletonRefresher");
    }

    static void setPathForSkeleton(String new_path) {
        m_path_for_skeleton_to_copy = new_path;
    }

    private void runCmdCommand(String command, @Nullable File directoryToRunIn) throws Exception {
        Process p = Runtime.getRuntime().exec(command, null, directoryToRunIn);
        int status = p.waitFor();
        if (status != 0) {
            throw new Exception(String.format("Failed to run command: %s", command));
        }

    }

    //this is a sudanichka method for finding the actual stubs location
    private int countDirs(File curDir) {
        int dirCount = 0;
        File[] filesInDir = curDir.listFiles();
        if (filesInDir == null)
            return 0;
        for (File curFile : filesInDir)
            if (curFile.isDirectory())
                dirCount++;
        return dirCount;
    }

    private Vector<String> getGeneratedSkeletonsPathByRegex(Vector<String> pycharm_dirs) throws IOException {
        String regex = "(?s)PyCharm.*(/system/python_stubs/-?[0-9]+)";
        Pattern ptrn = Pattern.compile(regex);
        Vector<String> skeletonPaths = new Vector<>();
        for (String path : pycharm_dirs) {
            String tablePath = path + "\\config\\options\\jdk.table.xml";
            File currentFile = new File(tablePath);
            if (!currentFile.exists())
                continue;
            String current_string = new String(Files.readAllBytes(Paths.get(tablePath)));
            Matcher matches = ptrn.matcher(current_string);
            if (matches.find() && matches.groupCount() == 1) {
                String currentMatch = path + matches.group(1);
                skeletonPaths.add(currentMatch.replace('/', '\\'));
            }
        }
        return skeletonPaths;
    }

    private Vector<String> getGeneratedSkeletonsPathByDirCount(Vector<String> stub_paths) {
        Vector<String> pathsOfSkeletons = new Vector<>();
        for (String stubPath : stub_paths) {
            File f = new File(stubPath);
            File[] innerDirectories = f.listFiles();
            if (innerDirectories == null)
                continue;
            for (File stubDir : innerDirectories)
                if (countDirs(stubDir) > 10)//random number to filter the actual skeleton dir
                    pathsOfSkeletons.add(stubDir.getAbsolutePath());
        }
        return pathsOfSkeletons;
    }

    private Vector<String> unionVectors(Vector<String> vec1, Vector<String> vec2) {
        if (vec1 == null || vec1.size() == 0)
            return vec2;
        if (vec2 == null || vec2.size() == 0)
            return vec1;
        TreeSet<String> hashedArray = new TreeSet<>();
        hashedArray.addAll(vec1);
        hashedArray.addAll(vec2);
        String[] unifiedObjects = hashedArray.toArray(new String[0]);
        return new Vector<>(Arrays.asList(unifiedObjects));
    }

    private void copyPydToSitePackagesDir(Sdk currentSdk) throws Exception {
        VirtualFile sdkHome = currentSdk.getHomeDirectory();
        if (sdkHome == null)
            throw new Exception(String.format("Failed to locate sdk home dir : %s", currentSdk.getName()));
        File sitePackagesFile = new File(sdkHome.getParent().getPath() + "\\Lib\\site-packages");
        File pathOfPyd = new File(m_path_for_skeleton_to_copy + "\\CompiledFiles\\Pyd");
        if (!sitePackagesFile.exists())
            throw new Exception(String.format("Cannot find dir: %s", sitePackagesFile.getAbsolutePath()));
        if (!pathOfPyd.exists())
            throw new Exception(String.format("Cannot find dir %s", pathOfPyd.getAbsolutePath()));
        runCmdCommand(String.format("xcopy %s %s /E /Y", pathOfPyd.getAbsolutePath(), sitePackagesFile.getAbsolutePath()), null);
    }


    private Vector<String> getGeneratedSkeletonsPath() {
        File dir = new File(System.getProperty("user.home"));
        File[] pyCharmDirs = dir.listFiles((dir1, name) -> name.contains("PyCharm"));
        Vector<String> pathsToStubs = new Vector<>();
        Vector<String> pathsToPycharm = new Vector<>();
        if (pyCharmDirs == null)
            return null;
        for (File pyCharmDir : pyCharmDirs)
            if (pyCharmDir.isDirectory()) {
                pathsToStubs.add(pyCharmDir.getAbsolutePath() + File.separator + "system\\python_stubs");
                pathsToPycharm.add(pyCharmDir.getAbsolutePath());
            }

        Vector<String> skeletonsPathsGeneratedByRegex;
        Vector<String> generatedSkeletonsPathByDirCount = getGeneratedSkeletonsPathByDirCount(pathsToStubs);
        try {
            skeletonsPathsGeneratedByRegex = getGeneratedSkeletonsPathByRegex(pathsToPycharm);
        } catch (IOException e) {
            skeletonsPathsGeneratedByRegex = null;
        }
        return unionVectors(skeletonsPathsGeneratedByRegex, generatedSkeletonsPathByDirCount);
    }

    private void copyContentsWhenDone(String skeleton_path) throws Exception {
        String fullPathCustomSkeletons = m_path_for_skeleton_to_copy + "\\Python\\ResultFiles";
        String pathToScript = m_path_for_skeleton_to_copy + "\\Python\\Parsers";
        File sourceDirectory = new File(fullPathCustomSkeletons);
        File destinationDirectory = new File(skeleton_path);
        File scriptFile = new File(pathToScript);
        if (!scriptFile.isDirectory()) {
            throw new Exception(String.format("Invalid directory  - %s", m_path_for_skeleton_to_copy));
        }
        runCmdCommand("python ClassParser.py", new File(pathToScript));
        if (!sourceDirectory.isDirectory()) {
            throw new Exception("Parsing script failed to generate ResultFiles folder");
        }
        runCmdCommand(String.format("xcopy %s %s /E /Y", sourceDirectory, destinationDirectory), null);
    }

    private void runnableFunction(ProgressIndicator indicator, Project proj, List<Sdk> sdkList) throws Exception {
        Vector<String> allSkeletonPaths = getGeneratedSkeletonsPath();
        indicator.setFraction(0);
        if (allSkeletonPaths == null)
            throw new Exception("Failed to locate any skeleton paths (consider re-regenerating and retry");
        int totalNumOfSdks = sdkList.size();
        int totalNumOfSkeletons = allSkeletonPaths.size();
        for (Sdk sdk : sdkList) {
            indicator.setText(String.format("Copying pyd to sdk %s", sdk.getHomeDirectory()));
            copyPydToSitePackagesDir(sdk);
            indicator.setFraction(indicator.getFraction() + (INDICATOR_PROGRESSION_AMOUNT / totalNumOfSdks));
            for (String skeletonsPath : allSkeletonPaths) {
                try {
                    indicator.setText(String.format("Refreshing skeletons for skeleton path %s", skeletonsPath));
                    PySkeletonRefresher.refreshSkeletonsOfSdk(proj, null, skeletonsPath, sdk);
                    indicator.setText("Copying custom skeletons");
                    indicator.setFraction(indicator.getFraction() + (INDICATOR_PROGRESSION_AMOUNT / (totalNumOfSdks * totalNumOfSkeletons)));
                    copyContentsWhenDone(skeletonsPath);
                    indicator.setText("Finished updating skeleton");
                    indicator.setFraction(indicator.getFraction() + (INDICATOR_PROGRESSION_AMOUNT / (totalNumOfSdks * totalNumOfSkeletons)));
                } catch (InvalidSdkException e) {
                    throw new Exception("Skeleton refresh failed! (Invalid SDK)");
                }
            }
        }
        indicator.setFraction(1);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        synchronized (SYNC_OBJECT) {
            e.getPresentation().setEnabled(!isGenerating);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getDataContext().getData(PlatformDataKeys.PROJECT);
        List<Sdk> allSdks = PythonSdkType.getAllSdks();
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "RE - Skeleton Generation") {
            public void run(@NotNull ProgressIndicator indicator) {
                synchronized (SYNC_OBJECT) {
                    isGenerating = true;
                }
                try {
                    runnableFunction(indicator, project, allSdks);
                    Notification notification = new Notification("RE", "RE", "Success: python-dds library is now available", NotificationType.INFORMATION);
                    Notifications.Bus.notify(notification);
                } catch (Exception e) {
                    Notification notification = new Notification("RE", "RE - Skeleton Generator ERROR", e.getMessage(), NotificationType.ERROR);
                    Notifications.Bus.notify(notification);
                } finally {
                    synchronized (SYNC_OBJECT) {
                        isGenerating = false;
                    }
                }
            }

        });
    }
}

