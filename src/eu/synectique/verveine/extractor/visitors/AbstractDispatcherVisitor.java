package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.extractor.plugin.CDictionary;


/**
 * The superclass of all visitors. These visitors visit an AST to create FAMIX entities.<BR>
 * This abstract class dispatches more finely the visits than what CDT's ASTVisitor normally do. 
 * This visitor also merges two APIs: visit methods on AST (ASTVisitor) and visit methods on ICElements (ICElementVisitor).
 */
public abstract class AbstractDispatcherVisitor extends ASTVisitor implements ICElementVisitor {

	/**
	 * CDT index to get AST and resolve symbols
	 */
	protected IIndex index;

	/** 
	 * A dictionary allowing to store created FAMIX Entities
	 * <p>
	 * This does not strictly belongs here, but all sub-classes need it,
	 * so it's simpler to have it in their super-class (this one)
	 */
	protected CDictionary dico;


	// CONSTRUCTOR ==========================================================================================================================

	public AbstractDispatcherVisitor(CDictionary dico, IIndex index) {
		super(/*visitNodes*/true);
		shouldVisitTemplateParameters = true;
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;
	    shouldVisit... */
		this.index = index;
		this.dico = dico;
	}


	// VISITING METODS ON ICELEMENT HIERARCHY (ICElementVisitor) ===========================================================================

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
		case ICElement.C_INCLUDE:
			visit( (IInclude) elt);
			break;

		default:
			//  I believe this should never happen
		}
		return false;
	}

	public void visit(ICProject project) {
		visitChildren(project);
	}

	public void visit(ICContainer cont) {
		visitChildren(cont);
	}

	public void visit(IInclude project) {
	}

	public void visit(ITranslationUnit elt) {
		// this is the method merging ICElementVisitor and ASTVisitor

		// visit children (ICElementVisitor) ...
		visitChildren(elt);
		try {
			// ... then visit AST (ASTVisitor)
			elt.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT | ITranslationUnit.AST_SKIP_INDEXED_HEADERS).accept(this);
		} catch (CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ elt.getElementName() );
		}
	}


	// CDT VISITING METODS ON AST (ASTVisitor) =============================================================================================

	@Override
	public int visit(IASTDeclaration node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because choices are not mutually exclusive
		 * ex: ICPPASTFunctionDefinition is a sub-interface of IASTFunctionDefinition
		 * ******************************************************************************************** */
		if (node instanceof IASTSimpleDeclaration) {
			return visit((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		else if (node instanceof IASTFunctionDefinition) {
			return visit((IASTFunctionDefinition)node);
		}
		else if (node instanceof ICPPASTTemplateDeclaration) {
			return visit((ICPPASTTemplateDeclaration)node);
		}
		else if (node instanceof ICPPASTVisibilityLabel) {
			return visit((ICPPASTVisibilityLabel)node);
		}
		//else ICPPASTUsingDirective, ...
	
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because choices are not mutually exclusive
		 * ex: ICPPASTFunctionDefinition is a sub-interface of IASTFunctionDefinition
		 * ******************************************************************************************** */
		if (node instanceof IASTSimpleDeclaration) {
			return leave((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		else if (node instanceof IASTFunctionDefinition) {
			return leave((IASTFunctionDefinition)node);
		}
		else if (node instanceof ICPPASTTemplateDeclaration) {
			return leave((ICPPASTTemplateDeclaration)node);
		}
		else if (node instanceof ICPPASTVisibilityLabel) {
			return leave((ICPPASTVisibilityLabel)node);
		}
		//else ICPPASTUsingDirective, ...
	
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclarator node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because choices are not mutually exclusive
		 * ex: ICPPASTFunctionDeclarator is a sub-interface of IASTFunctionDeclarator
		 * ******************************************************************************************** */
		if (node instanceof ICPPASTFunctionDeclarator) {
			return this.visit((ICPPASTFunctionDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			return this.visit((IASTFunctionDeclarator)node);
		}
		else if (node instanceof ICPPASTDeclarator) {
			return this.visit((ICPPASTDeclarator)node);
		}
	
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclarator node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because choices are not mutually exclusive
		 * ex: ICPPASTFunctionDeclarator is a sub-interface of IASTFunctionDeclarator
		 * ******************************************************************************************** */
		if (node instanceof ICPPASTFunctionDeclarator) {
			return this.leave((ICPPASTFunctionDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			return this.leave((IASTFunctionDeclarator)node);
		}
		else if (node instanceof ICPPASTDeclarator) {
			return this.leave((ICPPASTDeclarator)node);
		}
	
		if (node instanceof IASTFunctionDeclarator) {
			return this.leave((IASTFunctionDeclarator)node);
		}
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
		if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> struct/union
			return this.visit((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> class
			return this.visit((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTElaboratedTypeSpecifier) {
			return this.visit((IASTElaboratedTypeSpecifier)node);
		}
		else if (node instanceof IASTSimpleDeclSpecifier) {
			return this.visit((IASTSimpleDeclSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			return this.visit((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			return this.visit((ICPPASTNamedTypeSpecifier)node);
		}
		else {
			// TODO missing sub-types?
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
		else if (node instanceof IASTUnaryExpression) {
			return visit((IASTUnaryExpression)node);   // to check whether this is an assignement
		}
		else if (node instanceof IASTBinaryExpression) {
			return visit((IASTBinaryExpression)node);   // to check whether this is an assignement
		}
		else if (node instanceof IASTLiteralExpression) {
			return visit((IASTLiteralExpression)node);
		}
	
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTTemplateParameter node) {
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTTemplateParameter node) {
		return super.visit(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		if (node instanceof ICPPASTParameterDeclaration) {
			// not sure this can actually occur
			return visit((ICPPASTParameterDeclaration)node);
		}
		// else is a valid choice (presumably for C language)
		return super.visit(node);
	}

	@Override
	public int leave(IASTParameterDeclaration node) {
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		return super.visit(node);
	}


	// ADDITIONAL VISITING METODS ON AST ===================================================================================================

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

	protected int visit(ICPPASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTVisibilityLabel node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTVisibilityLabel node) {
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

	public int visit(IASTElaboratedTypeSpecifier node) {
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

	public int visit(IASTSimpleDeclSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTTemplateDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTTemplateDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionCallExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNewExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTUnaryExpression node) {
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

	protected void visitChildren(IParent elt) {
		try {
			for (ICElement child : elt.getChildren()) {
				child.accept(this);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}