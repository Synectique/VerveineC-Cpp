package eu.synectique.verveine.extractor.visitors.ref;

import java.io.RandomAccessFile;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.extractor.utils.Tracer;

public class RefVisitor extends AbstractRefVisitor implements ICElementVisitor {

	/**
	 * The source code of the visited AST.
	 * Used to find back the contents of non-javadoc comments
	 */
	protected RandomAccessFile source;

	/**
	 * Whether a variable access is lhs (write) or not
	 */
	protected boolean inAssignmentLHS = false;

	// CONSTRUCTOR ==========================================================================================================================

	/**
	 * Default constructor, dicoDef contains entities created during def pass
	 * @param dicoDef contains entities created during def pass
	 * @param dicoRef where entities are created during ref pass (the current pass)
	 * @param index CDT index containing bindings
	 */
	public RefVisitor(CDictionary dico, IIndex index) {
		super(dico, index, /*visitNodes*/true);

		tracer = new Tracer("REF>");
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

	/**
	 * Visiting a source file
	 */
	@Override
	public void visit(ITranslationUnit tu) {
		tracer.up("ITranslationUnit: "+tu.getElementName());
		context = new EntityStack2();    // "reseting" context
		super.visit(tu);
		tracer.down();
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		Namespace fmx;
		IASTName nodeName = node.getName();
		IIndexBinding bnd = null;

		nodeName = node.getName();

		try {
			bnd = index.findBinding(nodeName);
		}
		catch (CoreException e) {
			e.printStackTrace();
		}

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		fmx = (Namespace) dico.getEntityByKey(bnd);

		if (fmx != null) {
			this.context.push(fmx);
		}

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclaration node) {
		// includes CPPASTVisibilityLabel
//		tracer.msg("IASTDeclaration");
		if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		return new ParamDeclVisitor(dico, index, context).visit(node);
	}

	@Override
	public int visit(IASTName node) {
		referenceToName(((IASTName) node).getLastName());
		return ASTVisitor.PROCESS_SKIP;
	}


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		IASTName nodeName;
		IIndexBinding bnd = null;
		eu.synectique.verveine.core.gen.famix.Class fmx;

		nodeName = node.getName();
		tracer.up("ICPPASTCompositeTypeSpecifier:"+nodeName.toString());

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		fmx = (eu.synectique.verveine.core.gen.famix.Class) dico.getEntityByKey(bnd);

		if (fmx == null) {
			return PROCESS_SKIP;
		}
		
		// now looking for superclasses
		Inheritance lastInheritance = null;
		for (ICPPBase baseClass : ((ICPPClassType)bnd).getBases()) {
			eu.synectique.verveine.core.gen.famix.Class supFmx = null;
			IType supBnd = baseClass.getBaseClassType();

			if(supBnd instanceof IIndexBinding) {
				supFmx =  (eu.synectique.verveine.core.gen.famix.Class) dico.getEntityByKey((IIndexBinding) supBnd);
			}
			if (supFmx == null) {  // possibly as a consequence of (subBnd == null)
				// create a stub class
				supFmx = dico.ensureFamixClass(/*key*/null, /*name*/node.getBaseSpecifiers()[0].getNameSpecifier().toString(), /*owner*/null);
			}
			if (supFmx != null) {
					lastInheritance = dico.ensureFamixInheritance(supFmx, fmx, lastInheritance);
			}
		}

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	/**
	 * Visiting a method or function declaration
	 */
	@Override
	protected int visit(IASTFunctionDeclarator node) {
		IASTName nodeName;
		IIndexBinding bnd = null;
		BehaviouralEntity fmx = null;

		nodeName = node.getName();
		tracer.msg("IASTFunctionDeclarator: "+nodeName);

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		fmx = (BehaviouralEntity) dico.getEntityByKey(bnd);

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(IASTFunctionDeclarator node) {
		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof BehaviouralEntity) ) {
			context.pop();
		}
		return ASTVisitor.PROCESS_CONTINUE;
	}

	/**
	 * Visiting a method or function definition
	 */
	protected int visit(ICPPASTFunctionDefinition node) {
		/*
		 * visit declarator to ensure the method definition and to get the
		 * Famix entity (which will be on the top of the context stack)
		 */
		this.visit(node.getDeclarator());

		return PROCESS_CONTINUE;  // we already visited the children
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return this.leave(node.getDeclarator());
	}

	protected int visit(ICPPASTConstructorChainInitializer node) {
		return new FunctionCallVisitor(dico, index, context).visit(node);
	}

	protected int visit(ICPPASTConstructorInitializer node) {
		return new FunctionCallVisitor(dico, index, context).visit(node);
	}

	@Override
	protected int visit(IASTFunctionCallExpression node) {
		return new FunctionCallVisitor(dico, index, context).visit((IASTFunctionCallExpression)node);
	}

	@Override
	protected int visit(IASTLiteralExpression node) {
		if ( ((IASTLiteralExpression)node).getKind() == ICPPASTLiteralExpression.lk_this ) {
			if (context.topType() != null) {
				accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topMethod(), /*persistIt*/true));
			}
		}
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFieldReference node) {
		// can also be a method invocation
		((IASTFieldReference)node).getFieldOwner().accept(this);
		((IASTFieldReference) node).getFieldName().accept(this);
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTIdExpression node) {
		referenceToName(((IASTIdExpression) node).getName());
		return PROCESS_SKIP;
	}

	public int visit(IASTBinaryExpression node) {
		Access prevAccess = context.getLastAccess();
		node.getOperand1().accept(this);
		switch (node.getOperator()) {
		case IASTBinaryExpression.op_assign:
		case IASTBinaryExpression.op_binaryAndAssign:
		case IASTBinaryExpression.op_binaryOrAssign:
		case IASTBinaryExpression.op_binaryXorAssign:
		case IASTBinaryExpression.op_divideAssign:
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_moduloAssign:
		case IASTBinaryExpression.op_multiplyAssign:
		case IASTBinaryExpression.op_plusAssign:
		case IASTBinaryExpression.op_shiftLeftAssign:
		case IASTBinaryExpression.op_shiftRightAssign:
			if (context.getLastAccess() != prevAccess) {
				context.getLastAccess().setIsWrite(true);
			}
		}
		node.getOperand2().accept(this);
		
		return PROCESS_CONTINUE;
	}

}
