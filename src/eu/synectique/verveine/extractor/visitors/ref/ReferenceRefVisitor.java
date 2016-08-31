package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Reference;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;

public class ReferenceRefVisitor extends AbstractRefVisitor {

	/**
	 * set in visit(IASTUnaryExpression) to be used when visiting the type operand
	 */
	private boolean inSizeofExpression = false;

	public ReferenceRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	protected String msgTrace() {
		return "recording references to classes";
	}

	@Override
	protected int visit(IASTTypeIdExpression node) {
		inSizeofExpression = (node.getOperator() == IASTTypeIdExpression.op_sizeof);
		node.getTypeId().accept(this);
		inSizeofExpression = false;

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTElaboratedTypeSpecifier node) {
		if (inSizeofExpression) {
			referencetoType(node.getName());
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
		if (inSizeofExpression) {
			referencetoType(node.getName());
		}

		return PROCESS_SKIP;
	}

	protected Reference referencetoType(IASTName name) {
		Reference ref = dico.addFamixReference(context.topBehaviouralEntity(), referedType(name),  context.getLastReference());
		context.setLastReference(ref);
		return ref;
	}

}
