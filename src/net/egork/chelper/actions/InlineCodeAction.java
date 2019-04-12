package net.egork.chelper.actions;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import net.egork.chelper.codegeneration.SolutionGenerator;
import net.egork.chelper.util.ExecuteUtils;
import net.egork.chelper.util.FileUtilities;
import net.egork.chelper.util.Utilities;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * @author egor@egork.net
 */
public class InlineCodeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        PsiElement element = dataContext.getData(LangDataKeys.PSI_ELEMENT);
        if (element instanceof PsiFile) {
            for (PsiElement child : element.getChildren()) {
                if (child instanceof PsiClass) {
                    element = child;
                    break;
                }
            }
        }
        if (!(element instanceof PsiClass)) {
            return;
        }
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            return;
        }
        VirtualFile file = psiFile.getVirtualFile();
        if (file == null) {
            return;
        }
        String inlinedSource = SolutionGenerator.inlineCode(e.getProject(), (PsiClass) element);
        if (inlinedSource != null) {
            ExecuteUtils.executeStrictWriteAction(new Runnable() {
                @Override
                public void run() {
                    OutputStream stream = null;
                    try {
                        stream = file.getOutputStream(null);
                        stream.write(inlinedSource.getBytes(Charset.forName("UTF-8")));
                        stream.close();
                        FileUtilities.synchronizeFile(file);
                        PsiFile pFile = PsiManager.getInstance(e.getProject()).findFile(file);
                        if (pFile == null) {
                            return;
                        }
                        ReformatCodeProcessor processor = new ReformatCodeProcessor(pFile, false);
                        processor.run();
                        FileUtilities.synchronizeFile(file);
                    } catch (IOException e1) {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            });
        }
    }



    @Override
    public void update(AnActionEvent e) {
        final DataContext dataContext = e.getDataContext();
        final Presentation presentation = e.getPresentation();

        final boolean enabled = isAvailable(dataContext);

        presentation.setVisible(enabled);
        presentation.setEnabled(enabled);
    }

    private boolean isAvailable(DataContext dataContext) {
        if (!Utilities.isEligible(dataContext)) {
            return false;
        }
        PsiElement element = dataContext.getData(LangDataKeys.PSI_ELEMENT);
        if (element instanceof PsiClass) {
            return true;
        }
        if (!(element instanceof PsiFile)) {
            return false;
        }
        VirtualFile file = ((PsiFile) element).getVirtualFile();
        return file != null && file.isWritable() && "java".equals(file.getExtension());
    }

}
