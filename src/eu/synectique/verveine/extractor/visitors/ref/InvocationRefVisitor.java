package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTImplicitNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTInitializerClause;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.UnknownVariable;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class InvocationRefVisitor extends AbstractRefVisitor {

	protected static final String EMPTY_ARGUMENT_NAME = "__Empty_Argument__";
	/**
	 * In a sequence of identifier, this allows to know what was the type of the previous identifier
	 * so that we can know where to look for the current identifier (or where to create a stub one)
	 */
	protected Type priorType;

	public InvocationRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	protected String msgTrace() {
		return "recording methods and functions invocations";
	}

	/*
	 * putting class definition on the context stack
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;

		/* Gets the key (IBinding) of the node to recover the famix type entity */
		super.visit(node);

		fmx = (Class) dico.getEntityByKey(nodeBnd);

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		 returnedEntity = null;

		// compute nodeName and binding
		super.visit(node);

		// just in case this is a definition and we already processed the declaration
		returnedEntity = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);
		// try harder
		if (returnedEntity == null) {
			returnedEntity = resolveBehaviouralFromName(node, nodeBnd);
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		returnedEntity = null;

		((ICPPASTFunctionDeclarator)node.getDeclarator()).accept(this);

		this.context.push((BehaviouralEntity)returnedEntity);

		for (ICPPASTConstructorChainInitializer init : node.getMemberInitializers()) {
			init.accept(this);
		}

		node.getBody().accept(this);

		this.context.pop();

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		nodeBnd = null;
		nodeName = null;
		returnedEntity = null;

		priorType = context.topType();
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
					returnedEntity = invocationOfBehavioural((BehaviouralEntity) fmx);
				}
				else if (fmx instanceof StructuralEntity) {
					// fmx is probably a pointer to a BehavioralEntity
					String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
					returnedEntity = (DereferencedInvocation) dereferencedInvocation( (StructuralEntity)fmx, stubSig);
					dico.addSourceAnchor(returnedEntity, filename, node.getFileLocation());
				}
			}
		}
		

		for (IASTInitializerClause icl : node.getArguments()) {
			icl.accept(this);
		}

		return PROCESS_SKIP;
	}

	private void visitArguments(IASTInitializerClause[] args) {
		for (IASTInitializerClause icl : args) {
			icl.accept(this);

			/*if (returnedEntity == null) {
				System.err.println("bug");
			}*/
			if (returnedEntity instanceof Association) {
				((Invocation)returnedEntity).addArguments((Association) returnedEntity);
			}
			else {
				// so that the order of arguments match exactly their corresponding parameters
				// we create a fake association for argument that we cannot resolve
				IBinding fakeBnd = StubBinding.getInstance(UnknownVariable.class, EMPTY_ARGUMENT_NAME);
				UnknownVariable fake = dico.ensureFamixUniqEntity(UnknownVariable.class, fakeBnd, EMPTY_ARGUMENT_NAME);
				((Invocation)returnedEntity).addArguments(dico.addFamixAccess(context.topBehaviouralEntity(), fake, /*isWrite*/false, /*prev*/null));
			}
		}
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
		returnedEntity = nameInSource(((IASTIdExpression) node).getName(), node.getParent());
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFieldReference node) {
		node.getFieldOwner().accept(this);   // TODO why this?

		returnedEntity = nameInSource(node.getFieldName(), node.getParent());

		return PROCESS_SKIP;
	}

	protected Invocation nameInSource(IASTName nodeName, IASTNode nodeParent) {
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

		if ( (fmx instanceof BehaviouralEntity) && (! isPointer) ) {
			return invocationOfBehavioural((BehaviouralEntity) fmx);
		}
		
		return null;
	}

}
