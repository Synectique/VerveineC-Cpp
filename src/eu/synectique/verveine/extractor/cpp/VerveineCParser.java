package eu.synectique.verveine.extractor.cpp;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.synectique.famix.CPPSourceLanguage;
import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;

public class VerveineCParser extends VerveineParser {

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
	
	protected CDictionary dico;
	protected String projName;

	public void parse() {
		// projName set in setOptions()
        ICProject project = createProject(projName);
        
        dico = new CDictionary(getFamixRepo());
        
		try {
			visitTranslationUnits(project.getAllSourceRoots());
		} catch (CModelException e) {
			System.err.println("*** Got CModelException (\""+ e.getMessage() +"\") while trying to getAllSourceRoots");
		}	
	}
	
	protected void visitTranslationUnits(ICElement[] elts) {
		for (ICElement icElt : elts) {
			if (icElt instanceof ICContainer) {
				try {
					visitTranslationUnits( ((ICContainer) icElt).getChildren());
				} catch (CModelException e) {
					System.err.println("*** Got CModelException (\""+ e.getMessage() +"\") while trying to getChildren of "+icElt.getElementName());
				}
			}
			else if (icElt instanceof ITranslationUnit) {
				try {
					IASTTranslationUnit ast = ((ITranslationUnit)icElt).getAST();
					ast.accept(new MainVisitor(dico));
				} catch (CoreException e) {
					System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of: "+ icElt.getElementName() );
				}
			}
			else {
				// don't know what it is, don't know what to do with it
			}
		}
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
