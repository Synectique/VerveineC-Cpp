package eu.synectique.verveine.extractor.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.CSourceLanguage;
import eu.synectique.verveine.core.gen.famix.CppSourceLanguage;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;
import eu.synectique.verveine.extractor.utils.Constants;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.visitors.IncludeVisitor;
import eu.synectique.verveine.extractor.visitors.def.AttributeGlobalVarDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.BehaviouralDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.CommentDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.NamespaceDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.PackageDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.PreprocessorStmtDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.TemplateParameterDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.TypeDefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.DeclaredTypeRefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.InheritanceRefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.InvocationAccessRefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.ReferenceRefVisitor;

public class VerveineCParser extends VerveineParser {
	public static final String WORKSPACE_NAME = "tempWS";

	public static final String DEFAULT_PROJECT_NAME = "tempProj";

	private static final String SOURCE_ROOT_DIR = "src";

	/**
	 * Directory where the project to analyze is located
	 */
	private String userProjectDir;

	/**
	 * Default include paths for Linux
	 */
    public static final String[] LINUX_DEFAULT_INCLUDE = new String[] {
			 "/usr/include" ,
			 "/usr/local/include"
    };

    /**
     * Name of a file containing list of include dirs
     */
    protected String includeConfigFile;

	/**
	 * Temporary variable to gather include paths from the command line.
	 */
	private List<String> argIncludes;

	/**
	 * Temporary variable to gather macros defined from the command line
	 */
	private Map<String,String> argDefined;

	/**
	 * Whether this is a "windows" project.
	 * "Windows" project have file names where the case is not significant,
	 * thus AFile.c is the same as AFILE.c or aFILE.c
	 */
	private boolean windows;

	/**
	 * Eclipse CDT indexer
	 */
	private IIndex index = null;

	/**
	 * flag telling whether we need to look for all possible include dir
	 */
	private boolean autoinclude;

	/**
	 * flag telling whether we want to create a C or a C++ model.
	 * Defaults to C++ (cModel == false)
	 */
	private boolean cModel;
	
	/**
	 * Prefix to remove from file names
	 */
	protected String projectPrefix = null;

	/**
	 * Dictionary used to create all entities. Contains a Famix repository
	 */
	private CDictionary dico;

	public VerveineCParser() {
		super();
		this.argIncludes = new ArrayList<String>();
		this.argDefined = new HashMap<String,String>();
		this.autoinclude = false;
		this.windows = false;
		this.cModel = false;
		this.includeConfigFile = null;
		this.userProjectDir = null;

		dico = new CDictionary(getFamixRepo());
	}

	public boolean parse() {
		Activator.log(IStatus.INFO, "Copying source files in local project");
        ICProject cproject = createEclipseProject(DEFAULT_PROJECT_NAME, userProjectDir);
        if (cproject == null) {
        	// could not create the project :-(
        	return false;
        }
        projectPrefix = cproject.getLocationURI().getPath() + File.separator + (windows ? SOURCE_ROOT_DIR.toLowerCase() : SOURCE_ROOT_DIR) + File.separator;

        configIndexer(cproject);
		computeIndex(cproject);

        try {
    		runAllVisitors(dico, cproject);

		} catch (CoreException e) {
			e.printStackTrace();
			return false;
		}

        return true;
	}

	private void runAllVisitors(CDictionary dico, ICProject cproject) throws CoreException {
		/*Having very specialized visitors helps because each one is simpler
		 * so it is worth the impact on execution time
		 * Note that the order is important, the visitors are not independent */

		IncludeVisitor incVisitor;
		incVisitor = new IncludeVisitor(dico, index, projectPrefix);
		cproject.accept(incVisitor);

        int nbUI = 0;
        for (@SuppressWarnings("unused") String ui : incVisitor.getUnresolvedIncludes()) {
        	nbUI++;
        }
        modelComment(nbUI + " unresolved includes:", incVisitor.getUnresolvedIncludes());
		incVisitor.reportUnresolvedIncludes();

		cproject.accept(new PackageDefVisitor(dico));
		if (!cModel) {
			cproject.accept(new NamespaceDefVisitor(dico, index, projectPrefix));
		}
		cproject.accept(new TypeDefVisitor(dico, index, projectPrefix));
		
		BehaviouralDefVisitor behavVisitor = new BehaviouralDefVisitor(dico, index, projectPrefix);		// must be after class definitions
		behavVisitor.setHeaderFiles(true);
		cproject.accept(behavVisitor);
		behavVisitor.setHeaderFiles(false);
		cproject.accept(behavVisitor);
		if (!cModel) {
			cproject.accept(new TemplateParameterDefVisitor(dico, index, projectPrefix));	// must be after method definitions (possible template)
		}
		cproject.accept(new AttributeGlobalVarDefVisitor(dico, index, projectPrefix));			// must be after class/struct/enum definitions

		if (!cModel) {
			cproject.accept(new InheritanceRefVisitor(dico, index, projectPrefix));
		}
		cproject.accept(new DeclaredTypeRefVisitor(dico, index, projectPrefix));
		cproject.accept(new InvocationAccessRefVisitor(dico, index, projectPrefix));
		cproject.accept(new ReferenceRefVisitor(dico, index, projectPrefix));

		cproject.accept(new CommentDefVisitor(dico, index, projectPrefix));
		cproject.accept(new PreprocessorStmtDefVisitor(dico, index, projectPrefix));
	}

	private void configWorkspace(IWorkspace workspace) {
		IWorkspaceDescription workspaceDesc = workspace.getDescription();
		workspaceDesc.setAutoBuilding(false); // we do not want the workspace to rebuild the project every time a new resource is added
		try {
			workspace.setDescription(workspaceDesc);
		} catch (CoreException exc) {
			Activator.log(IStatus.ERROR, "Error trying to set workspace description: " + exc.getMessage());
		}

	}

	private ICProject createEclipseProject(String projName, String sourcePath) {
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
				project.delete(/*deleteContent*/true, /*force*/true, Constants.NULL_PROGRESS_MONITOR);
				project.refreshLocal(IResource.DEPTH_INFINITE, Constants.NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			Activator.log(IStatus.ERROR, "Error deleting project path=" + project.getFullPath().toString());
			exc.printStackTrace();
		}

		IProjectDescription eclipseProjDesc = workspace.newProjectDescription(project.getName());
		eclipseProjDesc.setLocation(eclipseProjPath);

		try {
			project.create(eclipseProjDesc, Constants.NULL_PROGRESS_MONITOR);
			project.open(Constants.NULL_PROGRESS_MONITOR);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}

		try {
			// now we make it a C project
			CCorePlugin.getDefault().createCProject(eclipseProjDesc, project, Constants.NULL_PROGRESS_MONITOR, project.getName());
			if (!project.isOpen()) {
				project.open(Constants.NULL_PROGRESS_MONITOR);
			}
		} catch (Exception exc) {
			Activator.log(IStatus.ERROR, "Error ("+exc.getClass().getSimpleName()+") in Project creation: " + exc.getMessage());
			exc.printStackTrace();
		}

		ICProjectDescription cProjectDesc = CoreModel.getDefault().getProjectDescription(project, true);
		cProjectDesc.setCdtProjectCreated();

		File projSrc = new File(sourcePath);
		if (! projSrc.exists()) {
			Activator.log(IStatus.ERROR, "Project directory "+sourcePath+ " not found !");
			return null;
		}
		FileUtil.copySourceFilesInProject(project, SOURCE_ROOT_DIR, projSrc, /*toLowerCase*/windows);
		ICProjectDescriptionManager descManager = CoreModel.getDefault().getProjectDescriptionManager();
        try {
			descManager.updateProjectDescriptions(new IProject[] { project }, Constants.NULL_PROGRESS_MONITOR);
		} catch (CoreException e) {
			e.printStackTrace();
		}

        return CoreModel.getDefault().getCModel().getCProject(project.getName());
	}

	/**
	 * sets include path (system, given by user) and macros into the project
	 */
	private void configIndexer(ICProject proj) {
		IPath projPath = proj.getPath();
		List<String> includeFromConf=new ArrayList<>();
		IPathEntry[] oldEntries=null;
		try {			
			oldEntries = proj.getRawPathEntries();
		} catch (CModelException e) {
			e.printStackTrace();
			return;
		}

		if (includeConfigFile != null) {
			readIncludeConf(includeConfigFile, includeFromConf);
		}
		
		IPathEntry[] newEntries = new IPathEntry[
		                                         oldEntries.length +
		                                         LINUX_DEFAULT_INCLUDE.length +
		                                         argIncludes.size() +
		                                         includeFromConf.size() +
		                                         argDefined.size()];
		int i;

		/* include paths */
		for (i=0; i < oldEntries.length; i++) {
			newEntries[i] = oldEntries[i];
		}
		/* include paths */
		for (String path : LINUX_DEFAULT_INCLUDE) {
			newEntries[i] = CoreModel.newIncludeEntry(projPath, null, new Path(path), /*isSystemInclude*/true);
			i++;
		}
		/* include paths */
		for (String path : argIncludes) {
			newEntries[i] = CoreModel.newIncludeEntry(projPath, null, new Path(path), /*isSystemInclude*/false);
			i++;
		}
		/* include paths */
		for (String path : includeFromConf) {
			newEntries[i] = CoreModel.newIncludeEntry(projPath, null, new Path(path), /*isSystemInclude*/false);
			i++;
		}
		/* macros  defined */
		for (Map.Entry<String, String> macro : argDefined.entrySet()) {
			newEntries[i] = CoreModel.newMacroEntry(projPath, macro.getKey(), macro.getValue());
		}

		try {			
			proj.setRawPathEntries(newEntries, Constants.NULL_PROGRESS_MONITOR);

		} catch (CModelException e) {
			e.printStackTrace();
		}
	}

	private void readIncludeConf(String confFileName, List<String> lines) {
		BufferedReader read = null;
		try {
			read = new BufferedReader( new FileReader(confFileName));
		} catch (FileNotFoundException e) {
			Activator.log(IStatus.WARNING, "Could not read Include Paths configuration file: " + confFileName);
			e.printStackTrace();
		}

		if (read != null) {
			String line;
			try {
				while ( (line=read.readLine()) != null ) {
					lines.add(line);
				}
			} catch (IOException e) {
				Activator.log(IStatus.WARNING, "Problem reading Include Paths configuration file: " + confFileName);
				e.printStackTrace();
				lines = new ArrayList<>();
			}
		}

		try {
			read.close();
		} catch (IOException e) {
			// ignore
		}
	}

	private void computeIndex(ICProject cproject) {
		Activator.log(IStatus.INFO, "Indexing source files");

		IIndexManager imanager = CCorePlugin.getIndexManager();
		imanager.setIndexerId(cproject, "org.eclipse.cdt.core.fastIndexer");
        imanager.reindex(cproject);
        imanager.joinIndexer(IIndexManager.FOREVER, Constants.NULL_PROGRESS_MONITOR );
		try {
			this.index = imanager.getIndex(cproject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected SourceLanguage getMyLgge() {
		if (cModel) {
			return new CSourceLanguage();
		}
		else {
			return new CppSourceLanguage();
		}
	}

	public void setOptions(String[] args) {
		modelComment("Program call arguments:", Arrays.asList(args));

		int i = 0;
		while (i < args.length && args[i].trim().startsWith("-")) {
		    String arg = args[i++].trim();

			if (arg.equals("-h")) {
				usage();
			}
			else if (arg.startsWith("-c")) {
				cModel = true;
			}
			else if (arg.equals("-autoinclude")) {
				autoinclude = true;
			}
			else if (arg.equals("-includeconf")) {
				includeConfigFile = args[i++].trim();
			}
			else if (arg.startsWith("-I")) {
				argIncludes.add(arg.substring(2));
			}
			else if (arg.startsWith("-D")) {
				parseMacroDefinition(arg);
			}
			else if (arg.equals("-windows")) {
				windows = true;
			}
			else {
				int j = super.setOption(i - 1, args);
				if (j > 0) {     // j is the number of args consumed by super.setOption()
					i += j;      // advance by that number of args
					i--;         // 1 will be added at the beginning of the loop ("args[i++]")
				}
				else {
					Activator.log(IStatus.WARNING, "** Unrecognized option: " + arg);
					usage();
				}
			}
		}

		for ( ; i < args.length; i++) {
			userProjectDir = args[i];
			
			if (autoinclude) {
				for (String inc : FileUtil.gatherIncludeDirs(args[i])) {
					argIncludes.add(inc);					
				}
			}
		}
	}

	private void modelComment(String title, Iterable<String> values) {
		/* TODO deactivated for now:
		String cmt = title; 
		for (String v : values) {
			cmt += " " + v;
		}
		dico.createFamixComment(cmt);
		 */
	}

	private void parseMacroDefinition(String arg) {
		int i;
		String macro;
		String value;

		i = arg.indexOf('=');
		if (i < 0) {
			macro=arg.substring(2);  // remove '-D' at the beginning
			value = "";
		}
		else {
			macro = arg.substring(2, i);
			value = arg.substring(i+1);
		}
		argDefined.put(macro, value);
	}

	protected void usage() {
		Activator.log(IStatus.INFO,
				"Usage: VerveineC [<options>] <eclipse-Cproject-to-parse>\n" +
				"Recognized options:\n" +
				"      -h: prints this message\n" +
				"      -o <output-file-name>: changes the name of the output file (default: output.mse)\n" +
				//"      -D<macro>: defines a C/C++ macro");
				"      -I<include-dir>: adds a directory containing include files\n" +
				"      -includeconf <config-file>: adds the directories listed in config-file in the include paths\n" +
				"      -autoinclude: looks for _all_ directories containing .h/.hh files and add them in the include paths\n" +
				"      <eclipse-Cproject-to-parse>: directory containing the C/C++ project to export in MSE");
		Activator.stop();
	}

}
