package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPField;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.InvocationWithArgs;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class FunctionCallVisitor extends AbstractRefVisitor {

	/**
	 * In a sequence of identifier, this allows to know what was the type of the previous identifier
	 * so that we can know where to look for the current identifier (or where to create a stub one)
	 */
	protected Type priorType;

	// CONSTRUCTOR ==========================================================================================================================

	public FunctionCallVisitor(AbstractRefVisitor parentVisitor) {
		super(parentVisitor);

		tracer = new NullTracer("FCV>");
	}


	// VISITING METODS ON AST ===============================================================================================================

	/**
	 * This is one of entry points for this visitor
	 */
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		IBinding bnd = null;
		IASTName nodeName = null;
		Invocation invok = null;
		
		priorType = context.topType();
		IASTNode[] children = node.getFunctionNameExpression().getChildren();
		for (int i=0; i < children.length - 1; i++) {   // for all children save the last one (presumably the called function's name)
			children[i].accept(this);
		}
		
		// try to identify (or create if a stub) the Behavioural being invoked
		IASTNode lastChild = children[children.length - 1];
		if (lastChild instanceof IASTName) {
			nodeName = (IASTName)lastChild;
			bnd = getBinding( nodeName );

			if (bnd != null) {
				fmx = dico.getEntityByKey(bnd);
			}
			
			if (fmx == null) {
				// could not find it. Try to create a stub from the name (if we have one)
				if (nodeName != null) {
					String stubSig =  mkStubSig(nodeName.toString(), node.getArguments().length);
					fmx = dico.ensureFamixFunction(/*key*/StubBinding.getInstance(Function.class, nodeName.toString()+"__"+node.getArguments().length), nodeName.toString(), stubSig, /*container*/null);
					((Function)fmx).setNumberOfParameters(node.getArguments().length);
					// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
					// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
				}
			}
			else if (fmx instanceof eu.synectique.verveine.core.gen.famix.Class) {
				// found a class instead of a behavioral. May happen, for example in the case of a "throw ClassName(...)"
				String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
				fmx = dico.ensureFamixMethod(/*key*/StubBinding.getInstance(Method.class, fmx.getName()+"__"+node.getArguments().length), fmx.getName(), stubSig, priorType);
				((Method)fmx).setNumberOfParameters(node.getArguments().length);
				// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
				// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
			}

			// now create the invocation
			if (fmx != null) {
				if (fmx instanceof BehaviouralEntity) {
					invok = invocationOfBehavioural((BehaviouralEntity) fmx);
				}
				else if (fmx instanceof StructuralEntity) {
					// fmx is probably a pointer to a BehavioralEntity
					String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
					invok = (DereferencedInvocation) dereferencedInvocation( (StructuralEntity)fmx );
					invok.setSignature(stubSig);
				}
			}
		}

		returnedEntity = invok;
		visitArguments(node.getArguments());

		return PROCESS_SKIP;
	}

	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorChainInitializer node) {
		returnedEntity = null;
		if (node.getMemberInitializerId().resolveBinding() instanceof ICPPField) {
			// field initialization that results in an implicit call to the constructor
			// not used for now
		}
		else if (node.getMemberInitializerId().resolveBinding() instanceof ICPPConstructor) {
			node.getInitializer().accept(this);
		}
		else {
			// not too sure what we have
			// try to visit it anyway
			node.getInitializer().accept(this);
		}
		return PROCESS_SKIP;
	}

	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorInitializer node) {
		IASTImplicitNameOwner parent = (IASTImplicitNameOwner)node.getParent() ;
		NamedEntity fmx = null;
		Invocation invok = null;

		for (IASTImplicitName candidate : parent.getImplicitNames()) {
			IBinding bnd = null; 

			bnd = getBinding( candidate );

			if (bnd != null) {
				fmx = dico.getEntityByKey(bnd);
			}
			
			if (fmx != null) {
				if (fmx instanceof BehaviouralEntity) {
					break;  // we found one method matching the implicit constructor. We are happy for now.
				}
			}
		}

		// if we could not get it, try to create a meaningful stub
		if (fmx == null) {
			String mthName = null;
			if (parent.getImplicitNames().length > 0) {
				mthName = parent.getImplicitNames()[0].toString();
			}
			else if (node.getParent() instanceof ICPPASTConstructorChainInitializer) {
				mthName = ((ICPPASTConstructorChainInitializer)node.getParent()).getMemberInitializerId().toString();
			}
			if (mthName != null) {
				String stubSig =  mkStubSig(mthName, node.getArguments().length);
				fmx = dico.ensureFamixMethod(/*key*/StubBinding.getInstance(Method.class, mthName+"__"+node.getArguments().length), mthName, stubSig, priorType);
				((Method)fmx).setNumberOfParameters(node.getArguments().length);
				// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
				// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
			}
		}

		if (fmx != null) {
			invok = invocationOfBehavioural((BehaviouralEntity) fmx);
		}
		
		returnedEntity = invok;
		visitArguments(node.getArguments());

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTName node) {
		Association assoc = referenceToName(node.getLastName(), /*reference*/false);
		
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
				accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topBehaviouralEntity(), /*persistIt*/true));
				priorType = context.topType();
			}
		}
		return PROCESS_SKIP;
	}


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	@Override
	protected int visit(IASTIdExpression node) {
		boolean reference;
		reference = ( (node.getParent() instanceof ICPPASTUnaryExpression) &&
					  ( ((ICPPASTUnaryExpression)node.getParent()).getOperator() == ICPPASTUnaryExpression.op_amper) );
		returnedEntity = referenceToName(((IASTIdExpression) node).getName(), reference);
		return PROCESS_SKIP;
	}


	// UTILITIES ====================================================================================================================================

	private void visitArguments(IASTInitializerClause[] args) {
		for (IASTInitializerClause icl : args) {
			RefVisitor subVisitor = new RefVisitor(this);

			icl.accept(subVisitor);

			if (returnedEntity instanceof InvocationWithArgs) {
				if (subVisitor.returnedEntity() instanceof Association) {
					((InvocationWithArgs)returnedEntity).addArguments((Association) subVisitor.returnedEntity());
				}
				else {
					((InvocationWithArgs)returnedEntity).addArguments((Association)null);
				}
			}
		}
	}

	/**
	 * Forges a signature for stub BehaviouralEntities
	 * @return "name(&lt;parameters&gt;)" where parametres are substitued by "_" 
	 */
	protected String mkStubSig(String name, int nbParam) {
		String sig = name + "(";
		for (int i=0; i < nbParam-1; i++) {
			sig += "_," ;
		}
		if (nbParam > 0) {
			sig += "_)" ;
		}
		else {
			sig += ")" ;
		}
		return sig;
	}

}
