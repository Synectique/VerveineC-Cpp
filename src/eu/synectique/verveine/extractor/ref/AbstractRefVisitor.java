package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ISourceRoot;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.Tracer;

/**
 * AST Visitor that defines all the (Famix) entities of interest
 * Famix entities are stored in a Map along with the IBindings to which they correspond
 */
public abstract class AbstractRefVisitor extends ASTVisitor implements ICElementVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionaryRef dico;

	protected ITracer tracer = new NullTracer();  // no tracing by default

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

	@Override
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
		visitChildren(project);
	}

	public void visit(ICContainer cont) {
		visitChildren(cont);
	}

	public void visit(ITranslationUnit tu) {
		try {
			this.filename = tu.getFile().getRawLocation().toString();
			tu.getAST().accept(this);
			this.filename = null;
		} catch (CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ tu.getElementName() );
		}
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(IASTDeclarator node) {
		if (node instanceof IASTFunctionDeclarator) {
			return this.visit((IASTFunctionDeclarator)node);
		}
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// BE CAREFULL: The order is important because IASTFunctionDeclarator is a sub-interface of ICPPASTDeclarator
		else if (node instanceof ICPPASTDeclarator) {
			return this.visit((ICPPASTDeclarator)node);
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclarator node) {

		if (node instanceof IASTFunctionDeclarator) {
			return this.leave((IASTFunctionDeclarator)node);
		}
		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		// BE CAREFULL: The order is important because IASTFunctionDeclarator is a sub-interface of ICPPASTDeclarator
		else if (node instanceof ICPPASTDeclarator) {
			return this.leave((ICPPASTDeclarator)node);
		}

		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			return this.visit((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			return this.visit((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			return this.visit((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			return this.visit((ICPPASTNamedTypeSpecifier)node);
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclSpecifier node) {
		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			return this.leave((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			return this.leave((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			return this.leave((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			return this.leave((ICPPASTNamedTypeSpecifier)node);
		}

		return super.leave(node);
	}

	// ADDITIONAL VISITING METODS ON AST =======================================================================================================

	protected abstract int visit(IASTFunctionDeclarator node);

	protected abstract int visit(ICPPASTDeclarator node);

	protected abstract int visit(ICASTCompositeTypeSpecifier node);

	protected abstract int visit(ICPPASTCompositeTypeSpecifier node);

	protected abstract int visit(IASTEnumerationSpecifier node);

	protected abstract int visit(ICPPASTNamedTypeSpecifier node);

	protected abstract int leave(IASTFunctionDeclarator node);

	protected abstract int leave(ICPPASTDeclarator node);

	protected abstract int leave(ICASTCompositeTypeSpecifier node);

	protected abstract int leave(ICPPASTCompositeTypeSpecifier node);

	protected abstract int leave(IASTEnumerationSpecifier node);

	protected abstract int leave(ICPPASTNamedTypeSpecifier node);

	// UTILITIES ======================================================================================================

	private void visitChildren(IParent elt) {
		try {
			for (ICElement child : elt.getChildren()) {
				child.accept(this);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
