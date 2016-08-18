package eu.synectique.verveine.extractor.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.build.core.scannerconfig.CfgInfoContext;
import org.eclipse.cdt.build.internal.core.scannerconfig.CfgDiscoveredPathManager;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.make.core.scannerconfig.PathInfo;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.CSourceLanguage;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;
import eu.synectique.verveine.extractor.utils.Constants;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.visitors.def.CommentDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.DefVisitor;
import eu.synectique.verveine.extractor.visitors.def.NamespaceDefVisitor;
import eu.synectique.verveine.extractor.visitors.def.PackageDefVisistor;
import eu.synectique.verveine.extractor.visitors.ref.RefVisitor;

public class VerveineCParser extends VerveineParser {
	static final public String WORKSPACE_NAME = "tempWS";

	static final public String DEFAULT_PROJECT_NAME = "tempProj";

	private static final String SOURCE_ROOT_DIR = File.separator + DEFAULT_PROJECT_NAME;

	/**
	 * Directory where the project to analyze is located
	 */
	private String userProjectDir;

	/**
	 * Default include paths for Linux
	 */
    static final public String[] LINUX_DEFAULT_INCLUDE = new String[] {
			 "/usr/include",
			 "/usr/local/include" ,
			 "/usr/include/c++/5" ,
			 "/usr/include/c++/5/backward" ,
			 "/usr/include/x86_64-linux-gnu" ,
			 "/usr/include/x86_64-linux-gnu/c++/5" ,
			 "/usr/lib/gcc/x86_64-linux-gnu/5/include" ,
			 "/usr/lib/gcc/x86_64-linux-gnu/5/include-fixed"
    };

	/**
	 * Temporary variable to gather include paths from the command line.
	 */
	private List<String> argIncludes;

	/**
	 * Temporary variable to gather macros defined from the command line
	 */
	private Map<String,String> argDefined;

	/**
	 * Eclipse CDT indexer
	 */
	private IIndex index;

	/**
	 * A tracer for debugging
	 */
	private ITracer tracer;

	/**
	 * flag telling whether we need to look for all possible include dir
	 */
	private boolean autoinclude;

	public VerveineCParser() {
		super();
		this.argIncludes = new ArrayList<String>();
		this.argDefined = new HashMap<String,String>();
		this .autoinclude = false;
	}

	public void parse() {
		CDictionary dico = new CDictionary(getFamixRepo());

		tracer = new Tracer();
		tracer.msg("step 1 / 3: indexing");

        ICProject cproject = createEclipseProject(DEFAULT_PROJECT_NAME, userProjectDir);

        configIndexer(cproject);
		computeIndex(cproject);

        try {
    		runAllDefVisitors(dico, cproject);
	        runAllRefVisitors(dico, cproject);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void runAllDefVisitors(CDictionary dico, ICProject cproject) throws CoreException {
		tracer.msg("step 2 / 3: creating structural entities");
		cproject.accept(new CommentDefVisitor(dico, index));
		cproject.accept(new PackageDefVisistor(dico, index));
		cproject.accept(new NamespaceDefVisitor(dico, index));

		DefVisitor step2 = new DefVisitor(dico, index);
		step2.setVisitHeaders(true);
		cproject.accept(step2);
		step2.setVisitHeaders(false);
		cproject.accept(step2);
		if (step2.nbUnresolvedIncludes() > 0) {
			tracer.msg("There were "+step2.nbUnresolvedIncludes()+" unresolved includes");
		}
	}

	private void runAllRefVisitors(CDictionary dico, ICProject cproject) throws CoreException {
		tracer.msg("step 3 / 3: creating references");
		RefVisitor step3 = new RefVisitor(dico, index);
		step3.setVisitHeaders(true);
		cproject.accept(step3);
		step3.setVisitHeaders(false);
		cproject.accept(step3);
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
			System.err.println("Error ("+exc.getClass().getSimpleName()+") in Project creation: " + exc.getMessage());
			exc.printStackTrace();
		}

		ICProjectDescription cProjectDesc = CoreModel.getDefault().getProjectDescription(project, true);
		cProjectDesc.setCdtProjectCreated();

		FileUtil.copySourceFilesInProject(project, SOURCE_ROOT_DIR, new File(sourcePath));
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

		IPathEntry[] oldEntries=null;
		try {			
			oldEntries = proj.getRawPathEntries();
		} catch (CModelException e) {
			e.printStackTrace();
			return;
		}

		IPathEntry[] newEntries = new IPathEntry[oldEntries.length + LINUX_DEFAULT_INCLUDE.length + argIncludes.size() + argDefined.size()];
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

	private void computeIndex(ICProject cproject) {
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
		return new CSourceLanguage();
	}

	public void setOptions(String[] args) {
		int i = 0;
		while (i < args.length && args[i].trim().startsWith("-")) {
		    String arg = args[i++].trim();

			if (arg.equals("-h")) {
				usage();
			}
			else if (arg.equals("-autoinclude")) {
				autoinclude = true;
			}
			else if (arg.startsWith("-I")) {
				argIncludes.add(arg.substring(2));
			}
			else if (arg.startsWith("-D")) {
				parseMacroDefinition(arg);
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
			userProjectDir = args[i];
			
			if (autoinclude) {
				for (String inc : FileUtil.gatherIncludeDirs(args[i])) {
					argIncludes.add(inc);					
				}
			}
		}
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
		System.err.println("Usage: VerveineC [<options>] <eclipse-Cproject-to-parse>");
		System.err.println("Recognized options:");
		System.err.println("      -h: prints this message");
		System.err.println("      -o <output-file-name>: changes the name of the output file (default: output.mse)");
		//System.err.println("      -D<macro>: defines a C/C++ macro");
		System.err.println("      -I<include-dir>: adds a directory containing include files");
		System.err.println("      -autoinclude: looks for _all_ directories containing .h/.hh files and add them in the include paths");
		System.err.println("      <eclipse-Cproject-to-parse>: directory containing the C/C++ project to export in MSE");
		System.exit(0);
	}

}
