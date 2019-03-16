package SkeletonGenerator;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
        String newPath = Messages.showInputDialog("Enter absolute path to python-dds-wrapper directory", "Change Directory Path", Messages.getInformationIcon());
        if (newPath == null || newPath.isEmpty())
            return;
        File pathChecker = new File(newPath);
        if (pathChecker.exists() && pathChecker.isDirectory())
            SkeletonRefresher.setPathForSkeleton(newPath);
        else
            Notifications.Bus.notify(new Notification("RE", "RE", String.format("Failed! path not found: %s",newPath), NotificationType.ERROR));
    }
}
