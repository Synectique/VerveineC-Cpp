package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.extractor.utils.Tracer;

/**
 * AST Visitor that defines all the (Famix) entities of interest
 * Famix entities are stored in a Map along with the IBindings to which they correspond
 */
public abstract class AbstractRefVisitor extends ASTVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionaryRef dico;

	protected Tracer tracer = new Tracer();

	/** name of the current file (TranslationUnit) being visited
	 */
	protected String filename;

	public AbstractRefVisitor(CDictionaryRef dico) {
		this(dico, /*visitNodes*/true);
	}

	public AbstractRefVisitor(CDictionaryRef dico, boolean visitNodes) {
		super(visitNodes);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;
	    shouldVisit... */
	    this.dico = dico;
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ======================================================================================================

	public boolean visit(ICElement elt) {
		switch (elt.getElementType()) {
		case ICElement.C_PROJECT:
			visit( (ICProject) elt);
			break;
		case ICElement.C_CCONTAINER:
			visit( (ICContainer) elt);
			break;
		case ICElement.C_UNIT:
			visit( (ITranslationUnit) elt);
			break;
		default:
			//  don't know what it is, don't know what to do with it
		}
		
		return false;
	}

	public void visit(ICProject project) {
		try {
			for (ISourceRoot src : project.getAllSourceRoots()) {
				this.visit(src);
			}
		} catch (CModelException e) {
			System.err.println("*** Got CModelException (\""+ e.getMessage() +"\") while trying to getAllSourceRoots of project "+project.getElementName());
			e.printStackTrace();
		}
	}

	public void visit(ICContainer cont) {
		try {
			for (ICElement child : cont.getChildren()) {
				this.visit( child);
			}
		} catch (CModelException e) {
			System.err.println("*** Got CModelException (\""+ e.getMessage() +"\") while trying to getChildren of "+cont.getElementName());
			e.printStackTrace();
		}
	}

	public void visit(ITranslationUnit tu) {
		try {
			this.filename = tu.getElementName();
			tu.getAST().accept(this);
		} catch (CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ tu.getElementName() );
		}
	}

}
