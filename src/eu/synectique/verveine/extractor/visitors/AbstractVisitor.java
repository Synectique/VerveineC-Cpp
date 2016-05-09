package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;

/**
 * The superclass of all visitors. These visitors visit an AST to create FAMIX entities.<BR>
 * An important function of this abstract class is to dispatch more finely the visits than what CDT normally do. 
 * This visitor also merges two APIs: visit methods on AST (ASTVisitor) and visit methods on ICElements (ICElementVisitor).
 */
public abstract class AbstractVisitor extends ASTVisitor implements ICElementVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionary dico;

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack context;

	protected ITracer tracer = new NullTracer();  // no tracing by default

	/** name of the current file (TranslationUnit) being visited
	 */
	protected String filename;

	protected IIndex index;

	// CONSTRUCTOR ==========================================================================================================================

	protected AbstractVisitor(CDictionary dico) {
		this(dico, /*index*/null, /*visitNodes*/true);
	    this.dico = dico;
	}

	public AbstractVisitor(CDictionary dico, IIndex index) {
		this(dico, index, /*visitNodes*/true);
	}

	public AbstractVisitor(CDictionary dico, IIndex index, boolean visitNodes) {
		super(visitNodes);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;
	    shouldVisit... */
		this.index = index;
	    this.dico = dico;
	}


	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

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
			tu.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT | ITranslationUnit.AST_SKIP_INDEXED_HEADERS).accept(this);
			this.filename = null;
		} catch (CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ tu.getElementName() );
		}
	}



	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(IASTDeclaration node) {
		if (node instanceof IASTSimpleDeclaration) {
			return visit((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		//else ICPPASTUsingDirective, ...

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof IASTSimpleDeclaration) {
			return leave((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		//else ICPPASTUsingDirective, ...

		return super.leave(node);
	}

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

	@Override
	public int visit(IASTInitializer node) {
		if (node instanceof ICPPASTConstructorChainInitializer) {
			return visit( (ICPPASTConstructorChainInitializer)node );
		}
		else if (node instanceof ICPPASTConstructorInitializer) {
			return visit( (ICPPASTConstructorInitializer)node );
		}
		return super.visit(node);
	}

	@Override
	public int visit(IASTExpression node) {
		if (node instanceof IASTFieldReference) {
			return visit((IASTFieldReference)node);
		}
		else if (node instanceof IASTIdExpression) {
			return visit((IASTIdExpression)node);
		}
		else if (node instanceof ICPPASTNewExpression) {
			return visit((ICPPASTNewExpression)node);
		}
		else if (node instanceof IASTFunctionCallExpression) {
			return visit((IASTFunctionCallExpression)node);
		}
		else if (node instanceof IASTBinaryExpression) {
			return visit((IASTBinaryExpression)node);   // to check whether this is an assignement
		}
		else if (node instanceof IASTLiteralExpression) {
			return visit((IASTLiteralExpression)node);
		}

		return super.visit(node);
	}
	

	// ADDITIONAL VISITING METODS ON AST =======================================================================================================

	protected int visit(IASTSimpleDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTSimpleDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	public int visit(IASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	public int leave(IASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionCallExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNewExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTBinaryExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTLiteralExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFieldReference node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTIdExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTConstructorChainInitializer node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTConstructorInitializer node) {
		return PROCESS_CONTINUE;
	}

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
