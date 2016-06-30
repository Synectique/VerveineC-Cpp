package eu.synectique.verveine.extractor.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
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
import org.eclipse.core.runtime.Path;

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
	static final public String WORKSPACE_NAME = "tempWS";

	static final public String DEFAULT_PROJECT_NAME = "tempProj";

	private static final String SOURCE_ROOT_DIR = File.separator + DEFAULT_PROJECT_NAME;

	/**
	 * different types of files that need to be checked when copying the project 
	 */
	private static final int SOURCE_FILE = 0;
	private static final int IGNORE_FILE = 1;
	private static final int UNKNOWN_FILE = 2;

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	private String userProjectDir;

	private String[] additionalIncludePath;
	
	private IIndex index;

	private ITracer tracer;

	public void parse() {
		CDictionary dico = new CDictionary(getFamixRepo());

		tracer = new Tracer();
		tracer.msg("step 1 / 3: indexing");

        IProject project = createEclipseProject(DEFAULT_PROJECT_NAME);
        ICProject cproject = initializeCProject(project, userProjectDir);

        try {
    		tracer.msg("step 2 / 3: creating structural entities");
            DefVisitor step2 = new DefVisitor(dico, index);
            step2.setVisitHeaders(true);
			cproject.accept(step2);
	        step2.setVisitHeaders(false);
	        cproject.accept(step2);
	        if (step2.nbUnresolvedIncludes() > 0) {
	        	tracer.msg("There were "+step2.nbUnresolvedIncludes()+" unresolved includes");
	        }

	        tracer.msg("step 3 / 3: creating references");
	        RefVisitor step3 = new RefVisitor(dico, index);
            step3.setVisitHeaders(true);
			cproject.accept(step3);
            step3.setVisitHeaders(false);
			cproject.accept(step3);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void configWorkspace(IWorkspace workspace) {
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(false); // we do not want the workspace to rebuild the project every time a new resource is added
		try {
			workspace.setDescription(workspaceDesc);
		} catch (CoreException exc) {
			System.err.println("Error trying to set workspace description: " + exc.getMessage());
		}

	}

	private IProject createEclipseProject(String projName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		configWorkspace(workspace);
		IWorkspaceRoot root = workspace.getRoot();
		// we make a directory at the workspace root to copy source files
		IPath eclipseProjPath = root.getRawLocation().removeLastSegments(1).append(WORKSPACE_NAME).append(projName);
		eclipseProjPath.toFile().mkdirs();

		final IProject project = root.getProject(projName);
		try {
			// delete content if the project exists
			if (project.exists()) {
				project.delete(/*deleteContent*/true, /*force*/true, NULL_PROGRESS_MONITOR);
				project.refreshLocal(IResource.DEPTH_INFINITE, NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		IProjectDescription eclipseProjDesc = workspace.newProjectDescription(project.getName());
		eclipseProjDesc.setLocation(eclipseProjPath);

		try {
			project.create(eclipseProjDesc, NULL_PROGRESS_MONITOR);
			project.open(NULL_PROGRESS_MONITOR);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		
		try {
			// now we make it a C project
			CCorePlugin.getDefault().createCProject(eclipseProjDesc, project, NULL_PROGRESS_MONITOR, project.getName());
			if (!project.isOpen()) {
				project.open(NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			System.err.println("Error ("+exc.getClass().getSimpleName()+") in Project creation: " + exc.getMessage());
			exc.printStackTrace();
		}

		ICProjectDescription cProjectDesc = CoreModel.getDefault().getProjectDescription(project, true);
		cProjectDesc.setCdtProjectCreated();

		return project;
	}

	private ICProject initializeCProject(IProject project, String sourcePath) {
		copySourceFilesInProject(project, new File(sourcePath));
		ICProjectDescriptionManager descManager = CoreModel.getDefault().getProjectDescriptionManager();
        try {
			descManager.updateProjectDescriptions(new IProject[] { project }, NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		ICProject cproject = CoreModel.getDefault().getCModel().getCProject(project.getName());

		try {
			IPathEntry[] oldEntries = cproject.getRawPathEntries();
			IPathEntry[] newEntries = new IPathEntry[oldEntries.length + additionalIncludePath.length];
			int i;
			for (i=0; i < oldEntries.length; i++) {
				newEntries[i] = oldEntries[i];
			}
			for (String path : additionalIncludePath) {
				newEntries[i] = CoreModel.newIncludeEntry(project.getFullPath(), null, new Path(path), true);
				i++;
			}
			cproject.setRawPathEntries(newEntries, NULL_PROGRESS_MONITOR);
		} catch (CModelException e) {
			e.printStackTrace();
		}

		IIndexManager imanager = CCorePlugin.getIndexManager();
		imanager.setIndexerId(cproject, "org.eclipse.cdt.core.fastIndexer");
        imanager.reindex(cproject);
        imanager.joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor() );
		try {
			this.index = imanager.getIndex(cproject);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return cproject;
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

		// TODO this should not be hard coded
		additionalIncludePath = new String[] {
				 "/usr/include/c++/5" ,
				 "/usr/include/x86_64-linux-gnu/c++/5" ,
				 "/usr/include/c++/5/backward" ,
				 "/usr/lib/gcc/x86_64-linux-gnu/5/include" ,
				 "/usr/local/include" ,
				 "/usr/lib/gcc/x86_64-linux-gnu/5/include-fixed" ,
				 "/usr/include/x86_64-linux-gnu" ,
				 "/usr/include"	
		};

		for ( ; i < args.length; i++) {
			userProjectDir = args[i];
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
