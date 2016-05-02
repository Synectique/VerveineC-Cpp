package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.Tracer;

public class FunctionCallVisitor extends AbstractRefVisitor {

	/**
	 * In a sequence of identifier, this allows to know what was the type of the previous identifier
	 * so that we can know where to look for the current identifier (or where to create a stub one)
	 */
	protected Type priorType;
	
	// CONSTRUCTOR ==========================================================================================================================

	public FunctionCallVisitor(CDictionary dico, IIndex index, EntityStack2 context) {
		super(dico, index, /*visitNodes*/true);

		this.context = context;

		tracer = new Tracer("FCV>");
	}


	// VISITING METODS ON AST ===============================================================================================================

	/**
	 * This is one of entry points for this visitor
	 */
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		IIndexBinding bnd = null;
		
		priorType = context.topType();
		IASTNode[] children = node.getFunctionNameExpression().getChildren();
		for (int i=0; i < children.length - 1; i++) {   // for all children save the last one (presumably the called function's name)
			children[i].accept(this);
		}
		
		IASTNode lastChild = children[children.length - 1];
		if (lastChild instanceof IASTName) {
			try {
				bnd = index.findBinding( (IASTName)lastChild );
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (bnd != null) {
				fmx = dico.getEntityByKey(bnd);
			}
			
			if (fmx != null) {
				if (! (fmx instanceof BehaviouralEntity) ) {
					fmx = dico.ensureMethod(/*key*/null, fmx.getName(), fmx.getName()+"(...)", priorType);
					fmx.setIsStub(true);
					invocationOfBehavioural((BehaviouralEntity) fmx);
				}
			}
		}
		return PROCESS_CONTINUE;
	}

	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorChainInitializer node) {
		return PROCESS_CONTINUE;
	}


	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorInitializer node) {
		 //IASTDeclSpecifier typ = ((IASTSimpleDeclaration)(node.getParent().getParent())).getDeclSpecifier() ;

		return PROCESS_CONTINUE;
	}

	public int visit(ICPPASTLiteralExpression node) {
		//referenceToName(((IASTName) node).getLastName());
		return ASTVisitor.PROCESS_SKIP;
	}

	@Override
	public int visit(IASTName node) {
		Association assoc = referenceToName(node.getLastName());
		
		if (assoc == null) {
			// assume it should be a variable
			accessToVar(dico.createFamixUnknownVariable(node.toString(), context.top()));
			priorType = null;
		}

		return ASTVisitor.PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTLiteralExpression node) {
		if (node.getKind() == ICPPASTLiteralExpression.lk_this) {
			if (context.topType() != null) {
				accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topMethod(), /*persistIt*/true));
				priorType = context.topType();
			}
		}
		return PROCESS_CONTINUE;
	}


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================
	// only defined here because they are abstract. Normally they are not used in this visitor

	@Override
	protected int visit(IASTFieldReference node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTIdExpression node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTBinaryExpression node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTDeclarator node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(IASTFunctionDeclarator node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(ICPPASTDeclarator node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(IASTEnumerationSpecifier node) {
		return PROCESS_SKIP;
	}

	@Override
	protected int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_SKIP;
	}


}
