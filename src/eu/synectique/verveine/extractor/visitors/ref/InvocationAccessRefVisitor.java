package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTImplicitNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.UnknownVariable;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class InvocationAccessRefVisitor extends AbstractRefVisitor {

	protected static final String EMPTY_ARGUMENT_NAME = "__Empty_Argument__";
	/**
	 * In a sequence of identifier, this allows to know what was the type of the previous identifier
	 * so that we can know where to look for the current identifier (or where to create a stub one)
	 */


	public InvocationAccessRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	protected String msgTrace() {
		return "recording methods and functions invocations";
	}

	@Override
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		Invocation invok = null;
		nodeBnd = null;
		nodeName = null;
		returnedEntity = null;

		IASTNode[] children = node.getFunctionNameExpression().getChildren();
		for (int i=0; i < children.length - 1; i++) {   // for all children save the last one (presumably the called function's name)
			children[i].accept(this);
		}

		// try to identify (or create if a stub) the Behavioural being invoked
		IASTNode lastChild = children[children.length - 1];
		if (lastChild instanceof IASTName) {
			nodeName = (IASTName)lastChild;
			nodeBnd = getBinding( nodeName );

			if (nodeBnd != null) {
				fmx = dico.getEntityByKey(nodeBnd);
			}

			if (fmx == null) {
				// could not find it. Try to create a stub from the name (if we have one)
				if (nodeName != null) {
					fmx = makeStubBehavioural(nodeName.toString(), node.getArguments().length, /*isMethod*/false);
				}
			}
			else if (fmx instanceof eu.synectique.verveine.core.gen.famix.Class) {
				// found a class instead of a behavioral. May happen, for example in the case of a "throw ClassName(...)"
				fmx = makeStubBehavioural(fmx.getName(), node.getArguments().length, /*isMethod*/true);
			}

			// now create the invocation
			if (fmx != null) {
				if (fmx instanceof BehaviouralEntity) {
					invok = invocationOfBehavioural((BehaviouralEntity) fmx);
					dico.addSourceAnchor(invok, filename, node.getFileLocation());
				}
				else if (fmx instanceof StructuralEntity) {
					// fmx is probably a pointer to a BehavioralEntity
					String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
					invok = (DereferencedInvocation) dereferencedInvocation( (StructuralEntity)fmx, stubSig);
					dico.addSourceAnchor(invok, filename, node.getFileLocation());
				}
			}
		}
		
		// dealing with arguments
		for (IASTInitializerClause icl : node.getArguments()) {
			icl.accept(this);

			if (returnedEntity instanceof Association) {
				invok.addArguments((Association) returnedEntity);  // should be an Invocation
			}
			else {
				// so that the order of arguments match exactly their corresponding parameters
				// we create a fake association for argument that we cannot resolve
				IBinding fakeBnd = StubBinding.getInstance(UnknownVariable.class, EMPTY_ARGUMENT_NAME);
				UnknownVariable fake = dico.ensureFamixUniqEntity(UnknownVariable.class, fakeBnd, EMPTY_ARGUMENT_NAME);
				invok.addArguments(dico.addFamixAccess(context.topBehaviouralEntity(), fake, /*isWrite*/false, /*prev*/null));
			}		}

		return PROCESS_SKIP;
	}

	/**
	 * Other entry point for this visitor
	 */
	@Override
	protected int visit(ICPPASTConstructorChainInitializer node) {
		IASTName memberName = node.getMemberInitializerId();
		returnedEntity = null;
		nodeBnd = getBinding(memberName);
		node.getInitializer().accept(this);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTConstructorInitializer node) {
		IASTImplicitNameOwner parent = (IASTImplicitNameOwner)node.getParent() ;
		NamedEntity fmx = null;

		// if this is an implicit call to a constructor (through attribute initialization call)
		for (IASTImplicitName candidate : parent.getImplicitNames()) {
			IBinding constBnd = null; 

			constBnd = getBinding( candidate );

			if (constBnd != null) {
				fmx = dico.getEntityByKey(constBnd);

				if (fmx instanceof BehaviouralEntity) {
					break;  // we found one method matching the implicit constructor. We are happy for now.
				}
			}
		}

		// if we could not get it, try to create a meaningful stub
		if (fmx == null) {
			// get the name of the called constructor
			String mthName = null;
			if (parent.getImplicitNames().length > 0) {
				mthName = parent.getImplicitNames()[0].toString();
			}
			else if (parent instanceof ICPPASTConstructorChainInitializer) {
				if ( returnedEntity instanceof Attribute ) {   // set by visit(ICPPASTConstructorChainInitializer)
					mthName = ((Attribute)returnedEntity).getDeclaredType().getName();
				}
				else {
					mthName = ((ICPPASTConstructorChainInitializer)parent).getMemberInitializerId().toString();
				}
			}
			// create the constructor
			if (mthName != null) {
				fmx = makeStubBehavioural(mthName, node.getArguments().length, /*isMethod*/true);
			}
		}

		if (fmx != null) {
			returnedEntity = invocationOfBehavioural((BehaviouralEntity) fmx);
		}

		for (IASTInitializerClause icl : node.getArguments()) {
			icl.accept(this);
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTIdExpression node) {
		returnedEntity = associationToName(((IASTIdExpression) node).getName(), node.getParent());
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFieldReference node) {
		node.getFieldOwner().accept(this);   // TODO why this?

		returnedEntity = associationToName(node.getFieldName(), node.getParent());

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTLiteralExpression node) {
		returnedEntity = null;
		if ( node.getKind() == ICPPASTLiteralExpression.lk_this ) {
			if (context.topType() != null) {
				returnedEntity = accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topBehaviouralEntity()));
			}
			else if (context.topMethod() != null) {
				returnedEntity = accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topMethod().getParentType(), /*owner*/context.topBehaviouralEntity()));
			}
		}
		return PROCESS_SKIP;
	}

	@Override
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




	protected Association associationToName(IASTName nodeName, IASTNode nodeParent) {
		NamedEntity fmx = null;
		boolean isPointer;

		nodeBnd = getBinding(nodeName);

		if (nodeBnd != null) {
			fmx = dico.getEntityByKey(nodeBnd);
		}
		else {
			fmx = findInParent(nodeName.toString(), context.top(), /*recursive*/true);
		}
		
		isPointer = ( (nodeParent instanceof ICPPASTUnaryExpression) &&
				  ( ((ICPPASTUnaryExpression)nodeParent).getOperator() == ICPPASTUnaryExpression.op_amper) );

		if (fmx instanceof StructuralEntity) {
			return accessToVar((StructuralEntity) fmx);
		}
		else if ( (fmx instanceof BehaviouralEntity) && (! isPointer) ) {
			return invocationOfBehavioural((BehaviouralEntity) fmx);
		}
		
		return null;
	}

	/**
	 * Records an Access to a StructuralEntity and sets lastAccess attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the s) 
	 * @param fmx -- Accessed StructuralEntity
	 * @return the Access created
	 */
	protected Access accessToVar(StructuralEntity fmx) {
		BehaviouralEntity accessor;
		// put false to isWrite by default, will be corrected in the visitor
		accessor = this.context.topBehaviouralEntity();
		Access acc = dico.addFamixAccess(accessor, fmx, /*isWrite*/false, context.getLastAccess());
		context.setLastAccess(acc);
		return acc;
	}

}
