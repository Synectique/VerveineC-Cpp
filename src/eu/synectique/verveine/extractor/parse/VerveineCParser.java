package eu.synectique.verveine.extractor.parse;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.synectique.famix.CPPSourceLanguage;
import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.def.DefVisitor;
import eu.synectique.verveine.extractor.ref.CDictionaryRef;
import eu.synectique.verveine.extractor.ref.MainRefVisitor;

public class VerveineCParser extends VerveineParser {

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

	protected String projName;

	public void parse() {
		// projName set in setOptions()
        ICProject project = createProject(projName);
        
        
        try {
        	// 1st step: create structural entities
        	CDictionaryDef dicoDef = new CDictionaryDef(getFamixRepo());
			project.accept(new DefVisitor(dicoDef));
			// 2nd step: switch dictionnary (ref-key=position ; ref-key=binding)
			CDictionaryRef dicoRef = new CDictionaryRef(getFamixRepo());
			new DefToRefDictionariesVisitor(dicoDef,dicoRef).visit(project);
			dicoDef = null;  // free memory
			// 3rd step: create references to entities
			new MainRefVisitor(dicoRef).visit(project);
		} catch (CoreException e) {
			e.printStackTrace();
		}
        //new MainRefVisitor(dico).visit(project);
	}
	
	private ICProject createProject(String projName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		System.err.println("INFO: workspace located in: "+root.getLocation());

		ICProject proj;
		proj = CoreModel.getDefault().getCModel().getCProject(projName);
		try {
			proj.open(NULL_PROGRESS_MONITOR);
			proj.makeConsistent(NULL_PROGRESS_MONITOR);
		}
		catch (final CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while trying to open CppProject: "+ projName);
		}

		return proj;
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
			projName = args[i++];
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
