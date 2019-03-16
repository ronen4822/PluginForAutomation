package SkeletonGenerator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import static SkeletonGenerator.SkeletonRefresher.SYNC_OBJECT;
import static SkeletonGenerator.SkeletonRefresher.isGenerating;


public class SkeletonPathChanger extends AnAction {
    public void update(AnActionEvent e) {
        synchronized (SYNC_OBJECT) {
            e.getPresentation().setEnabled(!isGenerating);
        }
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String txt = Messages.showInputDialog("Enter absolute path to python-dds-wrapper directory", "Change Directory Path", Messages.getInformationIcon());
        if (txt == null || txt.isEmpty())
            return;
        SkeletonRefresher.setPathForSkeleton(txt);
    }
}
