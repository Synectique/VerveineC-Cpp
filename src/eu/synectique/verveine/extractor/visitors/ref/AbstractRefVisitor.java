package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;
import eu.synectique.verveine.extractor.visitors.CDictionary;

/**
 * Abstract superclass for Reference visitors.<BR>
 * It defines some utility methods to create references to names.
 * It also adds a constructor accepting an existing context stack (see {@link AbstractVisitor#context}), this allows
 * to create specialized sub-visitors (e.g. {@link FunctionCallVisitor} or {@link ParamDeclVisitor}) while visiting an AST
 * with a "main" visitor.
 * @author anquetil
 */
public abstract class AbstractRefVisitor extends AbstractVisitor {

	// CONSTRUCTORS ==========================================================================================================================

	public AbstractRefVisitor(CDictionary dico) {
		super(dico);
	}

	public AbstractRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	public AbstractRefVisitor(CDictionary dico, IIndex index, boolean visitNodes) {
		super(dico, index, visitNodes);
	}

	public AbstractRefVisitor(CDictionary dico, IIndex index, EntityStack context, boolean visitNodes) {
		super(dico, index, visitNodes);
		this.context = context;
	}

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
	 * Records an Invocation of a BehaviouralEntity referenced by a variable (a pointer) and sets lastInvocation attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the invocation) 
	 * @param fmx -- StructuralEntity pointing to a BehaviouralEntity invoked
	 * @return the invocation created
	 */
	protected Association dereferencedInvocation(StructuralEntity fmx) {
		BehaviouralEntity accessor = this.context.topMethod();
		Invocation invok = dico.addFamixDereferencedInvocation(accessor, fmx, /*signature*/null, context.getLastInvocation());
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