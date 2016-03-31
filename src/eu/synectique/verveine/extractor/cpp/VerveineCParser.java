package eu.synectique.verveine.extractor.cpp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import eu.synectique.verveine.core.gen.famix.Entity;

public class VerveineCParser {

	private IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();
	
	Map<IBinding,Entity> dico;

	public void parse(String projName) {
        
        ICProject project = createProject(projName);
        //indexer = createIndex(project);
        
		try {
			findTranslationUnits(project.getAllSourceRoots());
		} catch (CModelException e) {
			System.err.println("*** Got CModelException (\""+ e.getMessage() +"\") while trying to getAllSourceRoots");
		}	
	}
	
	protected void findTranslationUnits(ICElement[] elts) {
		for (ICElement icElt : elts) {
			if (icElt instanceof ICContainer) {
				try {
					findTranslationUnits( ((ICContainer) icElt).getChildren());
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

}
