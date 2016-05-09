package eu.synectique.verveine.extractor.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.CSourceLanguage;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.visitors.CDictionary;
import eu.synectique.verveine.extractor.visitors.DefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.RefVisitor;

public class VerveineCParser extends VerveineParser {

	static final public String DEFAULT_PROJECT_NAME = "tempProj";

	private static final String SOURCE_ROOT_DIR = "/" + DEFAULT_PROJECT_NAME;

	/**
	 * different types of files that need to be checked when copying the project 
	 */
	private static final int SOURCE_FILE = 0;
	private static final int IGNORE_FILE = 1;
	private static final int UNKNOWN_FILE = 2;

	/**
	 * Indicates that an include path is system (as opposed to {@link ICSettingEntry#LOCAL}.
	 * Reversed engineered the value from CDT code.
	 */
	private static final int SYSTEM_INCLUDE_PATH = 0;

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	private String projectPath;

	private IIndex index;

	private ITracer tracer;

	public void parse() {
		CDictionary dico = null;

		System.out.println("step 1 / 3: indexing");

        IProject project = configureProject(createEclipseProject(DEFAULT_PROJECT_NAME));
		ICProject cproject = initializeCProject(project, projectPath);  // projPath set in setOptions()

        System.out.println("step 2 / 3: creating structural entities");
        dico = new CDictionary(getFamixRepo());
        DefVisitor step2 = new DefVisitor(dico, index);
        step2.setVisitHeaders(true);
        step2.visit(cproject);
        step2.setVisitHeaders(false);
        step2.visit(cproject);
        

        System.out.println("step 3 / 3: creating references");
        new RefVisitor(dico, index).visit(cproject);
	}

	/**
	 * Fill-in the project with source files and index these files
	 * @param project
	 * @param sourcePath
	 */
	private ICProject initializeCProject(IProject project, String sourcePath) {
		copySourceFilesInProject(project, new File(sourcePath));

		ICProjectDescriptionManager descManager = CoreModel.getDefault().getProjectDescriptionManager();
		try {
			descManager.updateProjectDescriptions(new IProject[] { project }, NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		ICProject cproject = CoreModel.getDefault().getCModel().getCProject(project.getName());


		// Joining the indexer ensures it is done indexing the project
		// We need this to start looking for references to entities
		// After that, we can get the indexer to ask it for bindings
        IIndexManager imanager = CCorePlugin.getIndexManager();
        imanager.joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor() );
       	try {
			this.index = imanager.getIndex(cproject);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		// everybody recommends to lock indexer in read.
		// not sure we really need it since we will not alter the source files ...
		try {
			this.index.acquireReadLock();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return cproject;
	}

	/**
	 * Creates an empty Project
	 * @param projName
	 * @return the IProject created
	 */
	private IProject createEclipseProject(String projName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();   // raises an org.eclipse.core.internal.resources.ResourceException if workspace directory does not exist
		IWorkspaceRoot root = workspace.getRoot();
		// we make a directory at the workspace root to copy source files
		IPath projectWSPath = workspace.getRoot().getRawLocation().removeLastSegments(1).append("workspace").append(projName);
		
		if (projectWSPath != null) {
			new File(projectWSPath.toOSString()).mkdirs();
		}

		final IProject project = root.getProject(projName);
		try {
			// delete if the project exists
			if (project.exists()) {
				project.delete(/*deleteContent*/true, /*force*/true, NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
		
		return project;
	}

	/**
	 * Creates an empty CProject
	 * @param projectName
	 * @return the project created
	 */
	private IProject configureProject(IProject project) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject cProject = null;
		
		// change a bit workspace description
		// TODO couldn't we do this on the project description ?
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(false); // we do not want the workspace to rebuild the project every time a new resource is added
		try {
			workspace.setDescription(workspaceDesc);
		} catch (CoreException exc) {
			System.err.println("Error trying to set workspace description: " + exc.getMessage());
		}

		IProjectDescription projectDesc = workspace.newProjectDescription(project.getName());
		IPath projectWSPath = workspace.getRoot().getRawLocation().removeLastSegments(1).append("workspace").append(project.getName());
		if (projectWSPath != null) {
			projectDesc.setLocation(projectWSPath); // point path to workspace
		}

		try {
			// now we create a C project for real
			cProject = CCorePlugin.getDefault().createCProject(projectDesc, project, NULL_PROGRESS_MONITOR, project.getName());
			if (!cProject.isOpen()) {
				cProject.open(NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			System.err.println("Error ("+exc.getClass().getSimpleName()+") in Project creation: " + exc.getMessage());
			exc.printStackTrace();
		}

		ICProjectDescription cprojectDesc = CoreModel.getDefault().getProjectDescription(project, true);
		ICConfigurationDescription configDecriptions[] = cprojectDesc.getConfigurations();
		for (ICConfigurationDescription configDescription : configDecriptions) {
			ICFolderDescription projectRoot = configDescription.getRootFolderDescription();

			for (ICLanguageSetting setting : projectRoot.getLanguageSettings()) {   // why a loop here ?
				List<ICLanguageSettingEntry> includes = new ArrayList<ICLanguageSettingEntry>();
				includes.addAll(setting.getSettingEntriesList(ICSettingEntry.INCLUDE_PATH));
				includes.add(new CIncludePathEntry("/usr/include", SYSTEM_INCLUDE_PATH));
				setting.setSettingEntries(ICSettingEntry.INCLUDE_PATH, includes);
			}
		}

		return cProject;
	}

	/**
	 * Copies all source files from src to the source directory of project
	 * @param project -- project where to copy the file(s)
	 * @param src -- A directory of file to copy to the project
	 */
	private void copySourceFilesInProject(IProject project, File src) {
		tracer = new NullTracer("Cpy");
		if (src.isDirectory()) {
			copySourceFilesRecursive(project, project.getFolder(SOURCE_ROOT_DIR), src);
		}
		else {
			copyFile(project, project.getFolder(SOURCE_ROOT_DIR), src);
		}
	}

	private void copySourceFilesRecursive(IProject project, IFolder internalPath, File dir) {
		tracer.up(dir.getAbsolutePath());
		if (checkFileType(dir.getName()) == IGNORE_FILE) {
			 tracer.msg("     ignore");
			return;
		}

		for (File child : dir.listFiles()) {
			if (child.isDirectory()) {
				copySourceFilesRecursive(project, internalPath.getFolder(child.getName()), child);
			}
			else {
				copyFile(project, internalPath, child);
			}
		}
		tracer.down();
	}

	/**
	 * Copies one source file in an Eclipse project to dest.
	 * If dest already exist, it is silently overriden
	 * @param project -- project where to copy the file
	 * @param orig -- file to copy in the project
	 * @param dest -- path within the project where to put the file
	 */
	private void copyFile(IProject project, IFolder destPath, File orig) {
		 tracer.msg(orig.getAbsolutePath());
		 if (checkFileType(orig.getName()) != SOURCE_FILE) {
			 tracer.msg("       not source");
			 return;
		 }
		 tracer.msg("       source to be copied");

		 if (! destPath.exists()) {
			mkdirs(destPath);
		}

		try {
			InputStream source = new ByteArrayInputStream( Files.readAllBytes(orig.toPath()) );
			IFile file = destPath.getFile(orig.getName());

			file.create(source, /*force*/true, NULL_PROGRESS_MONITOR);
			file.refreshLocal(IResource.DEPTH_ZERO, NULL_PROGRESS_MONITOR);

		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void mkdirs(IFolder destPath) {
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
			destPath.create(/*force*/true, /*local*/true, NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check whether a file name looks like a legitimate C/C++ source file
	 * @param filename
	 * @return
	 */
	private int checkFileType(String filename) {
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

	@Override
	protected SourceLanguage getMyLgge() {
		return new CSourceLanguage();
	}

	public void setOptions(String[] args) {
		int i = 0;
		while (i < args.length && args[i].trim().startsWith("-")) {
		    String arg = args[i++].trim();

			if (arg.equals("-h")) {
				usage();
			}
			else {
				int j = super.setOption(i - 1, args);
				if (j > 0) {     // j is the number of args consumed by super.setOption()
					i += j;      // advance by that number of args
					i--;         // 1 will be added at the beginning of the loop ("args[i++]")
				}
				else {
					System.err.println("** Unrecognized option: " + arg);
					usage();
				}
			}
		}

		for ( ; i < args.length; i++) {
			projectPath = args[i];
		}

	}

	protected void usage() {
		System.err.println("Usage: VerveineC [-h] [-o <output-file-name>] <eclipse-Cproject-to-parse>");
		System.err.println("      [-h] prints this message");
		System.err.println("      [-o <output-file-name>] specifies the name of the output file (default: output.mse)");
		System.err.println("      <eclipse-Cproject-to-parse> existing Eclipse C-project to export in MSE");
		System.exit(0);
	}

}
