package net.egork.chelper.util;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class FileUtilities {
	public static Properties loadProperties(VirtualFile file) {
		InputStream is = getInputStream(file);
		if (is == null)
			return null;
		Properties properties = new Properties();
		try {
			properties.load(is);
			return properties;
		} catch (IOException e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (IOException ignored) {
			}
		}
	}

	private static InputStream getInputStream(VirtualFile file) {
		try {
			return file.getInputStream();
		} catch (IOException e) {
			return null;
		}
	}

	public static PsiDirectory getDirectory(DataContext dataContext) {
		IdeView view = getView(dataContext);
		if (view == null)
			return null;
		PsiDirectory[] directories = view.getDirectories();
		if (directories.length != 1)
			return null;
		return directories[0];
	}

	public static IdeView getView(DataContext dataContext) {
		return LangDataKeys.IDE_VIEW.getData(dataContext);
	}

	public static boolean isJavaDirectory(PsiDirectory directory) {
		return directory != null && JavaDirectoryService.getInstance().getPackage(directory) != null;
	}

	public static VirtualFile writeTextFile(final VirtualFile location, final String fileName, final String fileContent)
	{
		ApplicationManager.getApplication().runWriteAction(new Runnable() {
			public void run() {
				if (location == null)
					return;
				OutputStream stream = null;
				try {
					VirtualFile file = location.createChildData(null, fileName);
					if (file == null)
						return;
					stream = file.getOutputStream(null);
					stream.write(fileContent.getBytes());
				} catch (IOException ignored) {
				} finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException ignored) {
						}
					}
				}
			}
		});
		if (location == null)
			return null;
		return location.findChild(fileName);
	}

	public static PsiDirectory getPsiDirectory(Project project, String location) {
		VirtualFile file = getFile(project, location);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findDirectory(file);
	}

	public static VirtualFile getFile(Project project, String location) {
		VirtualFile baseDir = project.getBaseDir();
		if (baseDir == null)
			return null;
		return baseDir.findFileByRelativePath(location);
	}

	public static String getRelativePath(VirtualFile baseDir, VirtualFile file) {
		if (file == null)
			return null;
		if (baseDir == null)
			return file.getPath();
		return file.getPath().substring(baseDir.getPath().length());
	}

	public static Point getLocation(Project project, Dimension size) {
		JComponent component = WindowManager.getInstance().getIdeFrame(project).getComponent();
		Point center = component.getLocationOnScreen();
		center.x += component.getWidth() / 2;
		center.y += component.getHeight() / 2;
		center.x -= size.getWidth() / 2;
		center.y -= size.getHeight() / 2;
		return center;
	}

	public static String getPackage(PsiDirectory directory) {
		PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(directory);
		String packageName = null;
		if (aPackage != null)
			packageName = aPackage.getQualifiedName();
		return packageName;
	}

	public static String getFQN(Project project, PsiDirectory directory, String name) {
		String packageName = getPackage(directory);
		if (packageName == null || packageName.length() == 0)
			return name;
		return packageName + "." + name;
	}

	public static PsiFile getPsiFile(Project project, String location) {
		VirtualFile file = getFile(project, location);
		if (file == null)
			return null;
		return PsiManager.getInstance(project).findFile(file);
	}

	public static VirtualFile createDirectoryIfMissing(Project project, String location) {
		VirtualFile baseDir = project.getBaseDir();
		if (baseDir == null)
			return null;
		try {
			return VfsUtil.createDirectoryIfMissing(baseDir, location);
		} catch (IOException e) {
			return null;
		}
	}

	public static void synchronizeFile(VirtualFile file) {
		FileDocumentManager.getInstance().saveDocument(FileDocumentManager.getInstance().getDocument(file));
	}
}
