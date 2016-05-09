package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTImplicitName;
import org.eclipse.cdt.core.dom.ast.IASTImplicitNameOwner;
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
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.visitors.CDictionary;

public class FunctionCallVisitor extends AbstractRefVisitor {

	/**
	 * In a sequence of identifier, this allows to know what was the type of the previous identifier
	 * so that we can know where to look for the current identifier (or where to create a stub one)
	 */
	protected Type priorType;

	// CONSTRUCTOR ==========================================================================================================================

	public FunctionCallVisitor(CDictionary dico, IIndex index, EntityStack context) {
		super(dico, index, context, /*visitNodes*/true);

		tracer = new NullTracer("FCV>");
	}


	// VISITING METODS ON AST ===============================================================================================================

	/**
	 * This is one of entry points for this visitor
	 */
	public int visit(IASTFunctionCallExpression node) {
		NamedEntity fmx = null;
		IIndexBinding bnd = null;
		IASTName nodeName = null;
		
		priorType = context.topType();
		IASTNode[] children = node.getFunctionNameExpression().getChildren();
		for (int i=0; i < children.length - 1; i++) {   // for all children save the last one (presumably the called function's name)
			children[i].accept(this);
		}
		
		IASTNode lastChild = children[children.length - 1];
		if (lastChild instanceof IASTName) {
			nodeName = (IASTName)lastChild;
			try {
				bnd = index.findBinding( nodeName );
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (bnd != null) {
				fmx = dico.getEntityByKey(bnd);
			}
			
			if (fmx == null) {
				// could not find it. Try to create a stub from the name (if we have one)
				if (nodeName != null) {
					String stubSig =  mkStubSig(nodeName.toString(), node.getArguments().length);
					fmx = dico.ensureFamixFunction(/*key*/StubBinding.getInstance(Method.class, stubSig), nodeName.toString(), stubSig, /*container*/null);	
				}
			}
			else if (fmx instanceof eu.synectique.verveine.core.gen.famix.Class) {
				// found a class instead of a behavioral. May happen, for example in the case of a "throw ClassName(...)"
				String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
				fmx = dico.ensureFamixMethod(/*key*/StubBinding.getInstance(Method.class, stubSig), fmx.getName(), stubSig, priorType);
			}

			if (fmx != null) {
				if (fmx instanceof BehaviouralEntity) {
					invocationOfBehavioural((BehaviouralEntity) fmx);
				}
				else if (fmx instanceof StructuralEntity) {
					// fmx is probably a pointer to a BehavioralEntity
					String stubSig =  mkStubSig(fmx.getName(), node.getArguments().length);
					DereferencedInvocation invok = (DereferencedInvocation) dereferencedInvocation( (StructuralEntity)fmx );
					invok.setSignature(stubSig);
					
				}
			}
		}

		return PROCESS_CONTINUE;
	}

	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorChainInitializer node) {
		return PROCESS_SKIP;
	}

	/**
	 * Other entry point for this visitor
	 */
	protected int visit(ICPPASTConstructorInitializer node) {
		IASTImplicitNameOwner parent = (IASTImplicitNameOwner)node.getParent() ;
		NamedEntity fmx = null;

		for (IASTImplicitName candidate : parent.getImplicitNames()) {
			IIndexBinding bnd = null; 

			try {
				bnd = index.findBinding( candidate );
			} catch (CoreException e) {
				e.printStackTrace();
			}

			if (bnd != null) {
				fmx = dico.getEntityByKey(bnd);
			}
			
			if (fmx != null) {
				if (fmx instanceof BehaviouralEntity) {
					break;  // we found one method matching the implicit constructor. We are happy for now.
				}
			}
		}

		if (fmx == null) {
			String mthName;
			if (parent.getImplicitNames().length > 0) {
				mthName = parent.getImplicitNames()[0].toString();
				String stubSig =  mkStubSig(mthName, node.getArguments().length);
				fmx = dico.ensureFamixMethod(/*key*/StubBinding.getInstance(Method.class, stubSig), mthName, stubSig, priorType);
			}
		}

		if (fmx != null) {
			invocationOfBehavioural((BehaviouralEntity) fmx);
		}

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


	// UTILITIES ====================================================================================================================================

	private String mkStubSig(String name, int nbParam) {
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
