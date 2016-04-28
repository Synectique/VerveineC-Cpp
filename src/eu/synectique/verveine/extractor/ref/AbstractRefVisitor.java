package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;

public abstract class AbstractRefVisitor extends AbstractVisitor {

	// CONSTRUCTORS ==========================================================================================================================

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack2 context;

	public AbstractRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	public AbstractRefVisitor(CDictionary dico, IIndex index, boolean visitNodes) {
		super(dico, index, visitNodes);
	}

	public AbstractRefVisitor(CDictionary dico) {
		super(dico);
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	/*
	 * Dispatches according to the actual type of the Expression
	 */
	@Override
	public int visit(IASTExpression node) {
		if (node instanceof IASTFieldReference) {
			return visit((IASTFieldReference)node);
		}
		else if (node instanceof IASTIdExpression) {
			return visit((IASTIdExpression)node);
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

	protected abstract int visit(IASTFunctionCallExpression node);

	protected abstract int visit(IASTBinaryExpression node);

	protected abstract int visit(IASTLiteralExpression node);

	protected abstract int visit(IASTFieldReference node);

	protected abstract int visit(IASTIdExpression node);

	// UTILITIES ==============================================================================================================================

	/**
	 * Records a reference to a name which can be a variable or behavioral name.
	 * @param nodeName
	 * @return the Access or Invocation created
	 */
	protected Association referenceToName(IASTName nodeName) {
		IIndexBinding bnd = null;
		NamedEntity fmx = null;

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd == null) {
			return null;
		}

		fmx = dico.getEntityByKey(bnd);

		if (fmx == null) {
			return null;
		}

		if (fmx instanceof StructuralEntity) {
			return accessToVar((StructuralEntity) fmx);
		}
		else if (fmx instanceof BehaviouralEntity) {
			return invocationOfBehavioural((BehaviouralEntity) fmx);
		}

		return null;
	}

	/**
	 * Records an Invocation of a famixBehaviouralEntity and sets lastInvocation attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the invocation) 
	 * @param fmx -- invoked BehaviouralEntity
	 * @return the invocation created
	 */
	protected Association invocationOfBehavioural(BehaviouralEntity fmx) {
		BehaviouralEntity accessor = this.context.topMethod();
		Invocation invok = dico.addFamixInvocation(accessor, (BehaviouralEntity) fmx, /*receiver*/null, /*signature*/null, context.getLastInvocation());
		if (invok != null) {
			context.setLastInvocation(invok);
		}
		return invok;
	}

	/**
	 * Records an Access to a StructuralEntity and sets lastAccess attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the s) 
	 * @param fmx -- Accessed StructuralEntity
	 * @return the Access created
	 */
	protected Association accessToVar(StructuralEntity fmx) {
		BehaviouralEntity accessor;
		// put false to isWrite by default, will be corrected in 
		accessor = this.context.topMethod();
		Access acc = dico.addFamixAccess(accessor, (StructuralEntity) fmx, /*isWrite*/false, context.getLastAccess());
		if (acc != null) {
			context.setLastAccess(acc);
		}
		return acc;
	}

}