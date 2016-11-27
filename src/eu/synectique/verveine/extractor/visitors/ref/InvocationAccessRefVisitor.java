package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTImplicitNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.UnknownVariable;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.QualifiedName;

public class InvocationAccessRefVisitor extends AbstractRefVisitor {

	protected static final String EMPTY_ARGUMENT_NAME = "__Empty_Argument__";


	/**
	 * set in visit(IASTUnaryExpression) to be used when visiting the operand
	 */
	private boolean inAmpersandUnaryExpression;

	protected boolean inCastExpression;

	public InvocationAccessRefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	@Override
	protected String msgTrace() {
		return "recording accesses to variables and invocations to methods/functions";
	}

	/**
	 * Overriden to initialize {@link #inAmpersandUnaryExpression} and  {@link #inCastExpr} to <code>false</code>
	 * (e.g. at the begining of a .c file) 
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		super.visit(elt);

		inAmpersandUnaryExpression = false;
		inCastExpression = false;
	}

	@Override
	protected int visit(IASTSimpleDeclaration node) {
		if (declarationIsTypedef(node)) {
			return PROCESS_SKIP;
		}
		else {
			return super.visit(node);
		}
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		if (! inCastExpression) {
			super.visit(node);
		}
		// else this is something like a cast to a fnction pointer type: do not handle it
		
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		Invocation invok = null;
		nodeBnd = null;
		nodeName = null;
		returnedEntity = null;

		IASTNode[] children = node.getFunctionNameExpression().getChildren();
		for (int i=0; i < children.length - 1; i++) {   // for all children except the last one (presumably the called function's name)
			children[i].accept(this);
		}

		// try to identify (or create if a stub) the Behavioural being invoked
		IASTNode lastChild = children[children.length - 1];
		if (lastChild instanceof IASTName) {
			nodeName = (IASTName)lastChild;
			nodeBnd = resolver.getBinding( nodeName );

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
					String stubSig =  resolver.mkStubSig(fmx.getName(), node.getArguments().length);
					invok = (DereferencedInvocation) dereferencedInvocation( (StructuralEntity)fmx, stubSig);
					dico.addSourceAnchor(invok, filename, node.getFileLocation());
				}
			}
		}

		// sometimes there is no function name in the node therefore, no fmx to invok
		// this happens when the result of the call is casted, this creates 2 IASTFunctionCallExpression
		// The parent holds the cast and has empty function name
		if (invok != null) {
			// dealing with arguments
			for (IASTInitializerClause icl : node.getArguments()) {
				icl.accept(this);

				if (returnedEntity instanceof Association) {
					invok.addArguments((Association) returnedEntity);
				}
				else {
					// so that the order of arguments match exactly their corresponding parameters
					// we create a fake association for argument that we cannot resolve
					IBinding fakeBnd = resolver.mkStubKey(EMPTY_ARGUMENT_NAME, UnknownVariable.class);
					UnknownVariable fake = dico.ensureFamixUniqEntity(UnknownVariable.class, fakeBnd, EMPTY_ARGUMENT_NAME);
					Access acc = dico.addFamixAccess(getContext().topBehaviouralEntity(), fake, /*isWrite*/false, /*prev*/null);
					invok.addArguments(acc);
					dico.addSourceAnchor(acc, filename, icl.getFileLocation());
				}
			}
		}

		return PROCESS_SKIP;
	}

	/**
	 * Other entry point for this visitor
	 */
	@Override
	protected int visit(ICPPASTConstructorChainInitializer node) {
		IASTName memberName = node.getMemberInitializerId();
		nodeBnd = resolver.getBinding(memberName);
		returnedEntity = dico.getEntityByKey(nodeBnd);
		if (returnedEntity == null) {
			Type parent = null;
			// top of context stack should be the constructor method that is ChainInitialized
			if (resolver.getContext().topMethod() != null) {
				parent = resolver.getContext().topMethod().getParentType();
			}
			// just in case, we look if the class of the constructor is not in the context stack ...
			else if (resolver.getContext().topType() != null) {
				parent = resolver.getContext().topType();
			}
			if (parent != null) {
				returnedEntity = resolver.findInParent(memberName.toString(), parent, /*recursive*/true);
			}
		}
		node.getInitializer().accept(this);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTConstructorInitializer node) {
		IASTImplicitNameOwner parent = (IASTImplicitNameOwner)node.getParent() ;
		NamedEntity fmx = null;

		// if this is an implicit call to a constructor
		for (IASTImplicitName candidate : parent.getImplicitNames()) {
			IBinding constBnd = null; 

			constBnd = resolver.getBinding( candidate );

			if (constBnd != null) {
				fmx = dico.getEntityByKey(constBnd);

				if (fmx instanceof BehaviouralEntity) {
					break;  // we found one method matching the implicit constructor. We are happy for now.
				}
			}
		}

		// if we could not get it, try to create a meaningful stub
		if (fmx == null) {
			// get the name of the called constructor (or attribute initialized)
			String mthName = null;
			if (parent.getImplicitNames().length > 0) {
				mthName = parent.getImplicitNames()[0].toString();
			}
			else if (parent instanceof ICPPASTConstructorChainInitializer) {
				// FIXME what if returnedType == null but should be Attribute ... ?
				if ( returnedEntity instanceof Attribute ) {    // hopefully set in visit(ICPPASTConstructorChainInitializer)
					mthName = ((Attribute)returnedEntity).getDeclaredType().getName();
				}
				else {
					// Constructor name is the name of its class (possibly fully qualified) + name of the class (unqualified) 
					QualifiedName qualName = new QualifiedName( ((ICPPASTConstructorChainInitializer)parent).getMemberInitializerId().toString() );
					mthName = qualName.toString() + QualifiedName.CPP_NAME_SEPARATOR + qualName.unqualifiedName();
				}
			}
			// create the constructor
			if (mthName != null) {
				fmx = makeStubBehavioural(mthName, node.getArguments().length, /*isMethod*/true);
			}
		}

		if (fmx != null) {
			returnedEntity = invocationOfBehavioural((BehaviouralEntity) fmx);
			dico.addSourceAnchor(returnedEntity, filename, node.getFileLocation());
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
		node.getFieldOwner().accept(this);   // to detect some field accesses

		returnedEntity = associationToName(node.getFieldName(), node.getParent());

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTLiteralExpression node) {
		returnedEntity = null;

		if ( node.getKind() == ICPPASTLiteralExpression.lk_this ) {
			if (getContext().topType() != null) {
				returnedEntity = accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/getContext().topType(), /*owner*/getContext().topBehaviouralEntity()));
			}
			else if (getContext().topMethod() != null) {
				returnedEntity = accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/getContext().topMethod().getParentType(), /*owner*/getContext().topBehaviouralEntity()));
			}
			if (returnedEntity != null) {
				dico.addSourceAnchor(returnedEntity, filename, node.getFileLocation());
			}
		}
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTUnaryExpression node) {
		inAmpersandUnaryExpression = (node.getOperator() == ICPPASTUnaryExpression.op_amper);
		node.getOperand().accept(this);
		inAmpersandUnaryExpression = false;

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

	@Override
	protected int visit(IASTCastExpression node) {
		inCastExpression = true;
		node.getTypeId().accept(this);
		inCastExpression = false;
		node.getOperand().accept(this);

		return PROCESS_SKIP;
	}

	protected Association associationToName(IASTName nodeName, IASTNode nodeParent) {
		NamedEntity fmx = null;
		Association assoc = null;

		nodeBnd = resolver.getBinding(nodeName);

		if (nodeBnd != null) {
			fmx = dico.getEntityByKey(nodeBnd);
		}
		else {
			fmx = resolver.findInParent(nodeName.toString(), getContext().top(), /*recursive*/true);
		}

		if (fmx instanceof StructuralEntity) {
			assoc = accessToVar((StructuralEntity) fmx);
		}
		else if (fmx instanceof BehaviouralEntity) { //&& (! inAmpersandUnaryExpression) ) {
			if (inAmpersandUnaryExpression) {
				return behaviouralPointer((BehaviouralEntity) fmx);
			}
			else {
				assoc = invocationOfBehavioural((BehaviouralEntity) fmx);
			}
		}
		if (assoc != null) {
			dico.addSourceAnchor(assoc, filename, nodeParent.getFileLocation());
		}
		
		return assoc;
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
		accessor = this.getContext().topBehaviouralEntity();
		Access acc = dico.addFamixAccess(accessor, fmx, /*isWrite*/false, getContext().getLastAccess());
		getContext().setLastAccess(acc);
		return acc;
	}

}
