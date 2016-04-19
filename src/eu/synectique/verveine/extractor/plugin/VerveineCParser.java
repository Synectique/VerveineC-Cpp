package eu.synectique.verveine.extractor.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexChangeEvent;
import org.eclipse.cdt.core.index.IIndexChangeListener;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
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

import eu.synectique.famix.CPPSourceLanguage;
import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.def.DefVisitor;
import eu.synectique.verveine.extractor.ref.CDictionaryRef;
import eu.synectique.verveine.extractor.ref.MainRefVisitor;

public class VerveineCParser extends VerveineParser {

	static final public String DEFAULT_PROJECT_NAME = "tempProj";

	private static final String SOURCE_ROOT_DIR = "/" + DEFAULT_PROJECT_NAME;

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	private String projectPath = "/home/anquetil/Documents/RMod/Tools/pluginzone/CodeExamples/simple/src";

	private IIndex index;

	public void parse() {
		CDictionaryDef dicoDef = null;
		CDictionaryRef dicoRef = null;

		System.out.println("step 1 / 3: indexing");

        ICProject project = createProject(DEFAULT_PROJECT_NAME, projectPath);  		// projPath set in setOptions()
        IIndexManager imanager = CCorePlugin.getIndexManager();
        //imanager.setIndexerId(project, "org.eclipse.cdt.core.fastIndexer");
        imanager.reindex(project);

		/*
		 * Joining the indexer ensures it is done indexing the project (well, I believe this is what it does anyway)
		 * We need this to start looking for references to entities
		 * It should be possible to run this in parallel with second step
		 * however doing this gives unpredictable results on the CDictionnaryDef.removeEntity() (some entities are found and removed, other not)
		 * Don't ask me why, I have no clue !!!
		 */
        imanager.joinIndexer(IIndexManager.FOREVER, new NullProgressMonitor() );
		try {
			this.index = imanager.getIndex(project);
			this.index.acquireReadLock();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

        try {
        	System.out.println("step 2 / 3: creating structural entities");
        	dicoDef = new CDictionaryDef(getFamixRepo());
			project.accept(new DefVisitor(dicoDef));
			
			System.out.println("step 3 / 3: creating references");
			dicoRef = new CDictionaryRef(getFamixRepo());
       		//dicoDef.sizes();
       		//dicoRef.sizes();
			new MainRefVisitor(dicoDef, dicoRef, index).visit(project);
			//dicoDef.sizes();
			//dicoRef.sizes();
		} catch (CoreException e) {
			e.printStackTrace();
		}
        
        if (! dicoDef.assertEmpty()) {
        	System.err.println("Possible problem: Def dictionnary was not emptied");
           	dicoDef.listAll(eu.synectique.verveine.core.gen.famix.Class.class);
           	dicoDef.listAll(Method.class);
           	dicoDef.listAll(Attribute.class);
        }
	}

	/**
	 * Creates a CProject from the source files in sourcePath
	 * @param projName
	 * @param sourcePath
	 * @return the ICProject created
	 */
	private ICProject createProject(String projName, String sourcePath) {
		IProject project = createNewProject(projName);

		//attachSourceFolder(project);
		copySourceFilesInProject(project, new File(sourcePath));
		
		ICProjectDescriptionManager descManager = CoreModel.getDefault().getProjectDescriptionManager();
		try {
			descManager.updateProjectDescriptions(new IProject[] { project }, NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		ICProject cproject = CoreModel.getDefault().getCModel().getCProject(project.getName());
		return cproject;
	}

	/**
	 * Creates an empty CProject
	 * @param projectName
	 * @return the project created
	 */
	private IProject createNewProject(String projectName) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// we make a directory at the workspace root to copy source files
		IPath projectWSPath = workspace.getRoot().getRawLocation().removeLastSegments(1).append("workspace").append(projectName);
		
		if (projectWSPath != null) {
			new File(projectWSPath.toOSString()).mkdirs();
		}

		final IProject project = root.getProject(projectName);
		try {
			// delete if the project exists
			if (project.exists()) {
				project.delete(/*deleteContent*/true, /*force*/true, NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		IProject cProject = null;
		
		// change a bit workspace description
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(false); // we do not want the workspace to rebuild the project every time a new resource is added
		try {
			workspace.setDescription(workspaceDesc);
		} catch (CoreException exc) {
			System.err.println("Error trying to set workspace description: " + exc.getMessage());
		}

		IProjectDescription projectDesc = workspace.newProjectDescription(project.getName());
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
		return cProject;
	}

	/**
	 * Copies all source files from src to the source directory of project
	 * @param project -- project where to copy the file(s)
	 * @param src -- A directory of file to copy to the project
	 */
	private void copySourceFilesInProject(IProject project, File src) {
		if (src.isDirectory()) {
			copySourceFilesRecursive(project, project.getFolder(SOURCE_ROOT_DIR), src);
		}
		else if (isValidFileExtension(src.getName())) {
			copyFile(project, project.getFolder(SOURCE_ROOT_DIR), src);
		}
		
	}

	private void copySourceFilesRecursive(IProject project, IFolder internalPath, File dir) {

		for (File child : dir.listFiles()) {
			if (child.isDirectory()) {
				copySourceFilesRecursive(project, internalPath.getFolder(child.getName()), child);
			}
			else if (isValidFileExtension(child.getName())) {
				copyFile(project, internalPath, child);
			}
		}
	}

	/**
	 * Copies one source file in an Eclipse project to dest.
	 * If dest already exist, it is silently overriden
	 * @param project -- project where to copy the file
	 * @param orig -- file to copy in the project
	 * @param dest -- path within the project where to put the file
	 */
	private void copyFile(IProject project, IFolder destPath, File orig) {
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
	private boolean isValidFileExtension(String filename) {
		String[] cppSourceExtensions = { ".cpp", ".hpp", ".hh", ".cc", ".icc", ".c", ".h" };
		for (String ext : cppSourceExtensions) {
			if (filename.endsWith(ext)) {
				return true;
			}
		}
		return false;

	}

	@Override
	protected SourceLanguage getMyLgge() {
		return new CPPSourceLanguage();
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

		while (i < args.length) {
			projectPath = args[i++];
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
