package SkeletonGenerator;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
public class SkeletonPathChanger extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String txt = Messages.showInputDialog("Please enter an absolute path to the python-dds-wrapper directory", "Change directory path", Messages.getInformationIcon());
        if (txt == null || txt.isEmpty())
            return;
        SkeletonRefresher.setPathForSkeleton(txt);
    }
}
