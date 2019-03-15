package SkeletonGenerator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import static SkeletonGenerator.SkeletonRefresher.sync_object;
import static SkeletonGenerator.SkeletonRefresher.is_generating;


public class SkeletonPathChanger extends AnAction {
    public void update(AnActionEvent e) {
        synchronized (sync_object) {
            e.getPresentation().setEnabled(!is_generating);
        }
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String txt = Messages.showInputDialog("Please enter an absolute path to the python-dds-wrapper directory", "Change Directory Path", Messages.getInformationIcon());
        if (txt == null || txt.isEmpty())
            return;
        SkeletonRefresher.setPathForSkeleton(txt);
    }
}
