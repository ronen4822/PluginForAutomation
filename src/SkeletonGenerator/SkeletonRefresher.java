package SkeletonGenerator;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.sdk.InvalidSdkException;
import org.jetbrains.annotations.NotNull;
import com.jetbrains.python.sdk.skeletons.PySkeletonRefresher;
import com.jetbrains.python.sdk.PythonSdkType;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Vector;

public class SkeletonRefresher extends AnAction {
    public SkeletonRefresher() {
        super("Text _Boxes");
    }

    //this is a sudanichka method for finding the actual stubs location
    private int countDirs(File cur_dir) {
        int dir_count = 0;
        File[] files_in_dir = cur_dir.listFiles();
        if (files_in_dir == null)
            return 0;
        for (File cur_file : files_in_dir) {
            if (cur_file.isDirectory()) {
                dir_count++;
            }
        }
        return dir_count;
    }

    private Vector<String> getGeneratedSkeletonsPath() {
        File dir = new File(System.getProperty("user.home"));
        File[] py_charm_dirs = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.contains("PyCharm");
            }
        });
        Vector<String> paths_to_stubs = new Vector<String>();
        if (py_charm_dirs == null)
            return null;
        for (File py_charm_dir : py_charm_dirs)
            if (py_charm_dir.isDirectory())
                paths_to_stubs.add(py_charm_dir.getAbsolutePath() + File.separator + "system\\python_stubs");

        Vector<String> paths_of_skeletons = new Vector<String>();
        for (String stub_path : paths_to_stubs) {
            File f = new File(stub_path);
            File[] inner_directories = f.listFiles();
            if (inner_directories == null)
                continue;
            for (File stub_dir : inner_directories)
                if (countDirs(stub_dir) > 5)//random number to filter the actual skeleton dir
                    paths_of_skeletons.add(stub_dir.getAbsolutePath());

        }
        return paths_of_skeletons;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        //String txt = Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
        //Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());
        Project project = anActionEvent.getDataContext().getData(PlatformDataKeys.PROJECT);
        List<Sdk> all_sdks = PythonSdkType.getAllSdks();
        //Vector<String> all_skeleton_paths = getGeneratedSkeletonsPath();
        //if (all_skeleton_paths == null)
        //    return;
        Vector<String>all_skeleton_paths=new Vector<String>();
        all_skeleton_paths.add("D:\\IDEAPlugin\\Sandbox\\system\\python_stubs\\-2036615904");
        for (Sdk sdk : all_sdks) {
            for (String skeletons_path : all_skeleton_paths) {
                try {
                    PySkeletonRefresher.refreshSkeletonsOfSdk(project,null,skeletons_path,sdk);
                } catch (InvalidSdkException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}

