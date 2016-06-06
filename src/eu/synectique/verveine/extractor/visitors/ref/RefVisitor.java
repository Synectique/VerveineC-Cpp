package eu.synectique.verveine.extractor.visitors.ref;

import java.io.RandomAccessFile;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;
import eu.synectique.verveine.extractor.visitors.CDictionary;
import eu.synectique.verveine.extractor.utils.NullTracer;
//import eu.synectique.verveine.extractor.utils.Tracer;

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
	 * Constructor for "main" visitor, dicoDef contains entities created during def pass
	 * @param dicoDef contains entities created during def pass
	 * @param dicoRef where entities are created during ref pass (the current pass)
	 * @param index CDT index containing bindings
	 */
	public RefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);

		tracer = new NullTracer("REF>");
	}

	/**
	 * Constructor when used as sub-visitor
	 */
	public RefVisitor(AbstractRefVisitor parent) {
		super(parent);

		tracer = new NullTracer("REF>");
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

	/**
	 * Visiting a source file
	 */
	@Override
	public void visit(ITranslationUnit tu) {
		tracer.up("ITranslationUnit: "+tu.getElementName());
		context = new EntityStack();    // "reseting" context
		super.visit(tu);
		tracer.down();
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		Namespace fmx;
	
		nodeName = node.getName();
		bnd = null;

		nodeName = node.getName();

		bnd = getBinding(nodeName);

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
		returnedEntity = this.context.pop();
		tracer.down();
		return super.leave(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		else if (node instanceof ICPPASTTemplateDeclaration) {
			return leave((ICPPASTTemplateDeclaration)node);
		}
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		Parameter fmx = null;

		 // get node name and bnd
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}
		fmx = (Parameter) dico.getEntityByKey(bnd);

		// now get the declared type
		if (fmx != null) {
			if (node.getParent() instanceof ICPPASTTemplateDeclaration) {
				// parameterType in a template
				// ignore for now
			}
			else {
				fmx.setDeclaredType( referedType( node.getDeclSpecifier() ) );
			}
		}
		returnedEntity = fmx;

		return PROCESS_SKIP;
	}


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		eu.synectique.verveine.core.gen.famix.Class fmx;

		// compute nodeName and binding
		super.visit(node);

		fmx = (eu.synectique.verveine.core.gen.famix.Class) dico.getEntityByKey(bnd);

		if (fmx == null) {
			return PROCESS_SKIP;
		}
		
		// now looking for superclasses
		if (bnd instanceof ICPPClassType) {
			// would not be the case if we had to create a StubBinding
			Inheritance lastInheritance = null;
			for (ICPPBase baseClass : ((ICPPClassType)bnd).getBases()) {
				Type supFmx = null;
				IType supBnd = baseClass.getBaseClassType();

				if(supBnd instanceof IIndexBinding) {
					supFmx =  (Type) dico.getEntityByKey((IIndexBinding) supBnd);
				}
				if (supFmx == null) {  // possibly as a consequence of (subBnd == null)
					//  may be should create a Type instead of a class here ?
					supFmx = createStubClass(/*name*/node.getBaseSpecifiers()[0].getNameSpecifier().toString());
				}
				if (supFmx != null) {
					lastInheritance = dico.ensureFamixInheritance(supFmx, fmx, lastInheritance);
				}
			}
		}
		
		this.context.push(fmx);
		
		for (IASTDeclaration child : node.getMembers()) {
			child.accept(this);
		}

		returnedEntity = this.context.pop();
		tracer.down();

		return PROCESS_SKIP;
	}

	/**
	 * Visiting an attribute to get its type
	 */
	@Override
	protected int visit(ICPPASTDeclarator node) {
		Attribute fmx = null;

		// compute nodeName and binding
		bnd = null;
		super.visit(node);

		if (bnd != null) {
			fmx = (Attribute) dico.getEntityByKey(bnd);
		}
		if (fmx != null) {
			// now get the declared type
			fmx.setDeclaredType( referedType( ((IASTSimpleDeclaration)node.getParent()).getDeclSpecifier() ) );
		}

		returnedEntity = fmx;

		return PROCESS_SKIP;
	}

	/**
	 * Visiting a method or function declaration
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		// compute nodeName and binding
		super.visit(node);

		fmx = (BehaviouralEntity) dico.getEntityByKey(bnd);

		this.context.push(fmx);
		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		returnedEntity = this.context.pop();

		return PROCESS_SKIP;  // already visited all we needed
	}

	/**
	 * Visiting a method or function definition
	 */
	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;


		// visit declarator to ensure the method definition and to get the Famix entity
		returnedEntity = null;
		this.visit( (ICPPASTFunctionDeclarator)node.getDeclarator() );

		fmx = (BehaviouralEntity) returnedEntity;
		if (fmx != null) {
			this.context.push((NamedEntity) this.returnedEntity());

			for (ICPPASTConstructorChainInitializer init : node.getMemberInitializers()) {
				init.accept(this);
			}

			// return type of the Behavioural
			fmx.setDeclaredType( referedType( node.getDeclSpecifier() ) );

			// body to compute some metrics
			node.getBody().accept(this);
			
			returnedEntity = context.pop();
		}

		return PROCESS_SKIP;  // we already visited the children
	}

	@Override
	protected int visit(ICPPASTConstructorChainInitializer node) {
		FunctionCallVisitor subVisitor = new FunctionCallVisitor(this);
		int retVal = subVisitor.visit(node);
		returnedEntity = subVisitor.returnedEntity();
		return retVal;
	}

	@Override
	protected int visit(ICPPASTConstructorInitializer node) {
		FunctionCallVisitor subVisitor = new FunctionCallVisitor(this);
		int retVal = subVisitor.visit(node);
		returnedEntity = subVisitor.returnedEntity();
		return retVal;
	}

	@Override
	protected int visit(IASTFunctionCallExpression node) {
		FunctionCallVisitor subVisitor = new FunctionCallVisitor(this);
		int retVal = subVisitor.visit(node);
		returnedEntity = subVisitor.returnedEntity();
		return retVal;
	}

	@Override
	protected int visit(IASTLiteralExpression node) {
		returnedEntity = null;
		if ( ((IASTLiteralExpression)node).getKind() == ICPPASTLiteralExpression.lk_this ) {
			if (context.topType() != null) {
				returnedEntity = accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topBehaviouralEntity(), /*persistIt*/true));
			}
		}
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFieldReference node) {
		// can also be a method invocation
		boolean reference;

		((IASTFieldReference)node).getFieldOwner().accept(this);
		
		reference = ( (node.getParent() instanceof ICPPASTUnaryExpression) &&
					  ( ((ICPPASTUnaryExpression)node.getParent()).getOperator() == ICPPASTUnaryExpression.op_amper) );
		returnedEntity = referenceToName(((IASTFieldReference) node).getFieldName(), reference);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTIdExpression node) {
		boolean isPointer;
		isPointer = ( (node.getParent() instanceof ICPPASTUnaryExpression) &&
					  ( ((ICPPASTUnaryExpression)node.getParent()).getOperator() == ICPPASTUnaryExpression.op_amper) );
		returnedEntity = referenceToName(((IASTIdExpression) node).getName(), isPointer);
		return PROCESS_SKIP;
	}

	public int visit(IASTBinaryExpression node) {
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
			if (this.returnedEntity() instanceof Access) {
				((Access) this.returnedEntity()).setIsWrite(true);
			}
		}
		node.getOperand2().accept(this);
		
		return PROCESS_SKIP;
	}


	// UTILITIES ==============================================================================================================================
	

	/**
	 * Call back method from {@link AbstractVisitor#visit(IASTSimpleDeclaration)}
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 */
	@Override
	protected void handleTypedef(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = (TypeAlias) dico.getEntityByKey(bnd);

		fmx.setAliasedType( referedType( node.getDeclSpecifier() ) );
		
		returnedEntity = fmx;
	}

	private Class createStubClass(String name) {
		IBinding supBnd;
		ContainerEntity parent;

		if (fullyQualified(name)) {
			parent = createParentNamespace(name);
			name = simpleName(name);
		}
		else {
			parent = getTopCppNamespace();
		}

		supBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName(parent, name));
		return dico.ensureFamixClass(supBnd, name, parent);
	}

}
