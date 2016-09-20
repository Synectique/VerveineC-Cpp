package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Reference;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;

public class ReferenceRefVisitor extends AbstractRefVisitor {

	/**
	 * set in visit(IASTUnaryExpression) to be used when visiting the type operand
	 */
	private boolean inSizeofExpression = false;

	private boolean inCastExpression = false;

	private boolean inTemplateArgumentExpression = false;

	public ReferenceRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	protected String msgTrace() {
		return "recording references to classes";
	}

	protected int visit(IASTCastExpression node) {
		inCastExpression = true;
		node.getTypeId().accept(this);
		inCastExpression = false;
		node.getOperand().accept(this);

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTTypeId node) {
		return super.visit(node);
	}

	@Override
	protected int visit(IASTTypeIdExpression node) {
		inSizeofExpression = (node.getOperator() == IASTTypeIdExpression.op_sizeof);
		node.getTypeId().accept(this);
		inSizeofExpression = false;

		return PROCESS_SKIP;
	}

	@Override
	public int visit(ICPPASTTemplateId node) {
		inTemplateArgumentExpression = true;
		for (IASTNode a : node.getTemplateArguments()) {
			a.accept(this);
		}
		inTemplateArgumentExpression = false;
		return PROCESS_SKIP;
	}

	public int visit(IASTSimpleDeclSpecifier node) {
		if (inSizeofExpression || inCastExpression || inTemplateArgumentExpression) {
			Reference ref = referenceToType( dico.ensureFamixPrimitiveType(node.getType()) );
			dico.addSourceAnchor(ref, filename, node.getFileLocation());
		}

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTElaboratedTypeSpecifier node) {
		if (inSizeofExpression || inCastExpression || inTemplateArgumentExpression) {
			Reference ref = referenceToType( referedType(node.getName()) );
			dico.addSourceAnchor(ref, filename, node.getFileLocation());
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTNamedTypeSpecifier node) {
		if (inSizeofExpression || inCastExpression || inTemplateArgumentExpression) {
			Reference ref = referenceToType( referedType(node.getName()) );
			dico.addSourceAnchor(ref, filename, node.getFileLocation());
		}

		return PROCESS_SKIP;
	}



	protected Reference referenceToType(Type referedType) {
		Reference ref = dico.addFamixReference(context.topBehaviouralEntity(), referedType,  context.getLastReference());
		context.setLastReference(ref);
		return ref;
	}

}
