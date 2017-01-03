package eu.synectique.verveine.extractor.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class FileUtil {

	/**
	 * different types of files that need to be checked when copying the project 
	 */
	private static final int SOURCE_FILE = 0;
	private static final int IGNORE_FILE = 1;
	private static final int UNKNOWN_FILE = 2;

	public static boolean isHeader(ITranslationUnit tu) {
		return isHeader(tu.getElementName());
	}

	public static boolean isHeader(File f) {
		return isHeader(f.getName());
	}

	public static boolean isHeader(String name) {
		String ext;
		int i = name.lastIndexOf('.');
		if (i < 0) {
			return false;    // no extension so not a header file
		}
		ext = name.substring(i);

		return ext.startsWith(".h");
	}

	public static String basename(String name) {
		int i = name.lastIndexOf(File.separatorChar);
		if (i < 0) {
			return name;
		}
		else {
			return name.substring(i+1);
		}
	}


	/**
	 * Copies all source files from src to the source directory of project
	 * @param project -- Eclipse project where to copy the file(s)
	 * @param src -- A directory of file to copy to the project
	 * @param destDir -- name of directory inside Eclipse project where to copy
	 * @param toLowerCase -- convert all file names to lower case (in windows, case is not important and might be inconsistent)
	 * @param addHExtension -- when an include does not specify an extension (#include <string>) adds a .h to help include resolver
	 */
	public static void copySourceFilesInProject(IProject project, String destDir, File src, boolean toLowerCase, boolean addHExtension) {
		if (toLowerCase) {
			destDir = destDir.toLowerCase();
		}

		if (src.isDirectory()) {
			copySourceFilesRecursive(project, project.getFolder(destDir), src, toLowerCase, addHExtension);
		}
		else {
			try {
				if (Files.isSymbolicLink(src.toPath()) && Files.readSymbolicLink(src.toPath()).toFile().isDirectory()) {
					copySourceFilesRecursive(project, project.getFolder(destDir), src, toLowerCase, addHExtension);
				}
				else {
					copyFile(project, project.getFolder(destDir), src, toLowerCase, addHExtension);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void copySourceFilesRecursive(IProject project, IFolder destPath, File dir, boolean toLowerCase, boolean addHExtension) {
		if (checkFileType(dir.getName()) == IGNORE_FILE) {
			return;
		}

		for (File child : dir.listFiles()) {
			String childName = null;
			if (child.isDirectory()) {
				if (toLowerCase) {
					childName = child.getName().toLowerCase();
				}
				else {
					childName = child.getName();
				}
				copySourceFilesRecursive(project, destPath.getFolder(childName), child, toLowerCase, addHExtension);
			}
			else {
				copyFile(project, destPath, child, toLowerCase, addHExtension);
			}
		}
	}

	/**
	 * Copies one source file in an Eclipse project to dest.
	 * If dest already exist, it is silently overridden
	 * @param project -- project where to copy the file
	 * @param orig -- file to copy in the project
	 * @param toLowerCase -- convert all file names to lower case (in windows, case is not important and might be inconsistent)
	 * @param dest -- path within the project where to put the file
	 */
	@SuppressWarnings("resource")
	private static void copyFile(IProject project, IFolder destPath, File orig, boolean toLowerCase, boolean addHExtension) {
		if (checkFileType(orig.getName()) != SOURCE_FILE) {
			return;
		}

		if (! destPath.exists()) {
			mkdirs(destPath);
		}

		try {
			String destName;
			if (toLowerCase) {
				destName = orig.getName().toLowerCase();
			}
			else {
				destName = orig.getName();
			}
			InputStream source = new ByteArrayInputStream( Files.readAllBytes(orig.toPath()) );
			IFile file = destPath.getFile(destName);

			if (toLowerCase) {
				source = new IncludeToLowerFilterStream(source);
			}

			if (addHExtension) {
				source = new IncludeWithHExtensionFilterStream(source);
			}

			file.create(source, /*force*/true, Constants.NULL_PROGRESS_MONITOR);
			source.close();

			file.refreshLocal(IResource.DEPTH_ZERO, Constants.NULL_PROGRESS_MONITOR);

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void mkdirs(IFolder destPath) {
		IContainer parent = destPath.getParent(); 
		if (! parent.exists()) {
			if (parent instanceof IFolder) {
				mkdirs((IFolder) parent);
			}
			else if (parent instanceof IProject) {
				mkdirs( ((IProject)parent).getFolder(".") );
			}
		}
		try {
			destPath.create(/*force*/true, /*local*/true, Constants.NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gathering paths to all sub-directories of <code>dir</code> that contain some header file
	 * @param main directory to look in
	 * @return a List of Strings representing the paths
	 */
	public static List<String> gatherIncludeDirs(String name) {
		File dir = new File(name);
		if (dir.exists() && dir.isDirectory()) {
			return gatherIncludeDirs(dir);
		}
		else {
			return Constants.EMPTY_STRING_LIST;
		}
	}

	private static List<String> gatherIncludeDirs(File dir) {
		boolean dirAdded = false;
		List<String> ret = new LinkedList<String>();

		for (File child : dir.listFiles()) {
			if (child.isDirectory()) {
				ret.addAll(gatherIncludeDirs(child));
			}
			else {  // it's a file
				if ( (! dirAdded) && isHeader(child) ) {
					ret.add(dir.getAbsolutePath());
					dirAdded = true;
				}
			}
		}

		return ret;
	}

	/**
	 * Check whether a file name looks like a legitimate C/C++ source file
	 * @param filename
	 * @return
	 */
	private static int checkFileType(String filename) {
		if (filename.charAt(0) == '.') {
			return IGNORE_FILE;
		}

		String[] cppSourceExtensions = { ".cpp", ".hpp", ".hh", ".cc", ".icc", ".c", ".h" };
		for (String ext : cppSourceExtensions) {
			if (filename.endsWith(ext)) {
				return SOURCE_FILE;
			}
		}

		return UNKNOWN_FILE;
	}




	public static RandomAccessFile openTranslationUnit(String filename) {
		RandomAccessFile br = null;
		try {
			br = new RandomAccessFile( filename, "r");
		} catch (FileNotFoundException e) {
			System.err.println("Error opening "+filename+" for reading");
		}

		return br;
	}

	public static String getFileContent( RandomAccessFile input, int start, int end) {
		byte buffer[] = new byte[ end-start+1];
		try {
			input.seek(start);
			int ret = input.read(buffer);
			if (ret < end-start+1) {
				System.err.println("missing bytes, read "+ret+" instead of "+(end-start+1));
				return "";
			}
			return buffer.toString();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String localized(String filename, String prefix) {
		if (filename.startsWith(prefix)) {
			return filename.substring(prefix.length());
		}
		else {
			return filename;
		}
	}
}
