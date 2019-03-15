package SkeletonGenerator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
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
import java.io.FilenameFilter;
import java.util.List;
import java.util.Vector;

public class SkeletonRefresher extends AnAction {
    private static String m_path_for_skeleton_to_copy = "C:\\git\\PythonDdsWrapper";
    static Boolean is_generating = false;
    static Object sync_object = new Object();

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
        File[] files_in_dir = curDir.listFiles();
        if (files_in_dir == null)
            return 0;
        for (File cur_file : files_in_dir)
            if (cur_file.isDirectory())
                dirCount++;
        return dirCount;
    }

    private Vector<String> getGeneratedSkeletonsPathByRegex(Vector<String> pycharm_dirs) throws IOException {
        String regex = "(?s)PyCharm.*(/system/python_stubs/-?[0-9]+)";
        Pattern ptrn = Pattern.compile(regex);
        Vector<String> skeleton_paths = new Vector<>();
        for (String path : pycharm_dirs) {
            String tablePath = path + "\\config\\options\\jdk.table.xml";
            File currentFile = new File(tablePath);
            if (!currentFile.exists())
                continue;
            String current_string = new String(Files.readAllBytes(Paths.get(tablePath)));
            Matcher matches = ptrn.matcher(current_string);
            if (matches.find() && matches.groupCount() == 1) {
                String currentMatch = path + matches.group(1);
                skeleton_paths.add(currentMatch.replace('/','\\'));
            }
        }
        return skeleton_paths;
    }

    private Vector<String> getGeneratedSkeletonsPathByDirCount(Vector<String> stub_paths) {
        Vector<String> paths_of_skeletons = new Vector<>();
        for (String stub_path : stub_paths) {
            File f = new File(stub_path);
            File[] inner_directories = f.listFiles();
            if (inner_directories == null)
                continue;
            for (File stub_dir : inner_directories)
                if (countDirs(stub_dir) > 10)//random number to filter the actual skeleton dir
                    paths_of_skeletons.add(stub_dir.getAbsolutePath());
        }
        return paths_of_skeletons;
    }

    private Vector<String> unionVectors(Vector<String> vec1, Vector<String> vec2) {
        if (vec1 == null || vec1.size() == 0)
            return vec2;
        if (vec2 == null || vec2.size() == 0)
            return vec1;
        TreeSet<String> hashedArray = new TreeSet<String>();
        hashedArray.addAll(vec1);
        hashedArray.addAll(vec2);
        String[] unifiedObjects = hashedArray.toArray(new String[0]);
        return new Vector<String>(Arrays.asList(unifiedObjects));
    }


    private Vector<String> getGeneratedSkeletonsPath() {
        File dir = new File(System.getProperty("user.home"));
        File[] py_charm_dirs = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains("PyCharm");
            }
        });

        Vector<String> paths_to_stubs = new Vector<>();
        Vector<String> paths_to_pycharm = new Vector<>();
        if (py_charm_dirs == null)
            return null;
        for (File py_charm_dir : py_charm_dirs)
            if (py_charm_dir.isDirectory()) {
                paths_to_stubs.add(py_charm_dir.getAbsolutePath() + File.separator + "system\\python_stubs");
                paths_to_pycharm.add(py_charm_dir.getAbsolutePath());
            }

        Vector<String> skeletons_paths_generated_by_regex;
        Vector<String> skeletons_paths_generated_by_dir_count = getGeneratedSkeletonsPathByDirCount(paths_to_stubs);
        try {
            skeletons_paths_generated_by_regex = getGeneratedSkeletonsPathByRegex(paths_to_pycharm);
        } catch (IOException e) {
            skeletons_paths_generated_by_regex = null;
        }
        return unionVectors(skeletons_paths_generated_by_regex, skeletons_paths_generated_by_dir_count);
    }

    private void copy_contents_when_done(String skeleton_path) throws Exception {
        String full_path_custom_skeletons = m_path_for_skeleton_to_copy + "\\Python\\ResultFiles";
        String path_to_script = m_path_for_skeleton_to_copy + "\\Python\\Parsers";
        File sourceDirectory = new File(full_path_custom_skeletons);
        File destinationDirectory = new File(skeleton_path);
        File script_file = new File(path_to_script);
        if (!script_file.isDirectory()) {
            throw new Exception(String.format("Invalid directory  - %s", m_path_for_skeleton_to_copy));
        }
        runCmdCommand("python ClassParser.py", new File(path_to_script));
        if (!sourceDirectory.isDirectory()) {
            throw new Exception("Parsing script failed to generate ResultFiles folder");
        }
        runCmdCommand(String.format("xcopy %s %s /E", sourceDirectory, destinationDirectory), null);
    }

    private void runnable_function(Project proj, List<Sdk> sdk_list) throws Exception {
        Vector<String> all_skeleton_paths = getGeneratedSkeletonsPath();
        if (all_skeleton_paths == null)
            throw new Exception("Failed to locate any skeleton paths (consider re-regenerating and retry");
        for (Sdk sdk : sdk_list) {
            for (String skeletons_path : all_skeleton_paths) {
                try {
                    PySkeletonRefresher.refreshSkeletonsOfSdk(proj, null, skeletons_path, sdk);
                    copy_contents_when_done(skeletons_path);
                } catch (InvalidSdkException e) {
                    throw new Exception("Skeleton refresh failed! (Invalid SDK)");
                }
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        synchronized (sync_object) {
            e.getPresentation().setEnabled(!is_generating);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getDataContext().getData(PlatformDataKeys.PROJECT);
        List<Sdk> all_sdks = PythonSdkType.getAllSdks();
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                synchronized (sync_object) {
                    is_generating = true;
                }
                try {
                    runnable_function(project, all_sdks);
                    Notification notification = new Notification("RE", "RE", "Success: files for auto-completion copied", NotificationType.INFORMATION);
                    Notifications.Bus.notify(notification);
                } catch (Exception e) {
                    Notification notification = new Notification("RE", "RE - Skeleton Generator ERROR", e.getMessage(), NotificationType.ERROR);
                    Notifications.Bus.notify(notification);
                } finally {
                    synchronized (sync_object) {
                        is_generating = false;
                    }
                }

            }
        });
    }
}

