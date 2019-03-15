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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

public class SkeletonRefresher extends AnAction {
    private static String m_path_for_skeleton_to_copy = "C:\\git\\PythonDdsWrapper";

    public SkeletonRefresher() {
        super("SkeletonRefresher");
    }

    public static void setPathForSkeleton(String new_path) {
        m_path_for_skeleton_to_copy = new_path;
    }

    //this is a sudanichka method for finding the actual stubs location
    private int countDirs(File cur_dir) {
        int dirCount = 0;
        File[] files_in_dir = cur_dir.listFiles();
        if (files_in_dir == null)
            return 0;
        for (File cur_file : files_in_dir) {
            if (cur_file.isDirectory()) {
                dirCount++;
            }
        }
        return dirCount;
    }

    private Vector<String> getGeneratedSkeletonsPath() {
        File dir = new File(System.getProperty("user.home"));
        File[] py_charm_dirs = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains("PyCharm");
            }
        });
        Vector<String> paths_to_stubs = new Vector<>();
        if (py_charm_dirs == null)
            return null;
        for (File py_charm_dir : py_charm_dirs)
            if (py_charm_dir.isDirectory())
                paths_to_stubs.add(py_charm_dir.getAbsolutePath() + File.separator + "system\\python_stubs");

        Vector<String> paths_of_skeletons = new Vector<>();
        for (String stub_path : paths_to_stubs) {
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

    private void copy_contents_when_done(String skeleton_path) throws Exception {
        String full_path_custom_skeletons = m_path_for_skeleton_to_copy + "\\Python\\ResultFiles";
        String path_to_script = m_path_for_skeleton_to_copy + "\\Python\\Parsers";
        File sourceDirectory = new File(full_path_custom_skeletons);
        File destinationDirectory = new File(skeleton_path);
        File script_file = new File(path_to_script);
        if (!script_file.isDirectory()) {
            throw new Exception(String.format("Invalid directory  - %s", m_path_for_skeleton_to_copy));
        }
        try {
            Runtime.getRuntime().exec("py ClassParser.py", null, new File(path_to_script));
        } catch (IOException e) {
            throw new Exception("Failed to execute parsing script");
        }
        if (!sourceDirectory.isDirectory()) {
            throw new Exception("Parsing script failed to generate ResultFiles folder");
        }
        try {
            Runtime.getRuntime().exec(String.format("xcopy %s %s /E", sourceDirectory, destinationDirectory));
        } catch (IOException e) {
            throw new Exception("Failed to copy files");
        }
    }

    private void runnable_function(Project proj, List<Sdk> sdk_list) throws Exception {
        Vector<String> all_skeleton_paths = getGeneratedSkeletonsPath();
        if (all_skeleton_paths == null)
            throw new Exception("Failed to locate any skeleton paths (consider re-regenerating and retry");
        //Vector<String> all_skeleton_paths = new Vector<String>();
        all_skeleton_paths.add("D:\\IDEAPlugin\\Sandbox\\system\\python_stubs\\-2036615904");
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
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getDataContext().getData(PlatformDataKeys.PROJECT);
        List<Sdk> all_sdks = PythonSdkType.getAllSdks();
        this.setEnabledInModalContext(false);
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable_function(project, all_sdks);
                    Notification notification = new Notification("RE", "RE", "Success: files for auto-completion copied", NotificationType.INFORMATION);
                    Notifications.Bus.notify(notification);
                } catch (Exception e) {
                    Notification notification = new Notification("RE", "RE - Skeleton Generator ERROR", e.getMessage(), NotificationType.ERROR);
                    Notifications.Bus.notify(notification);
                }

            }
        });
    }
}

