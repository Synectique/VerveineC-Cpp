package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateId;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;

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

	public ReferenceRefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	@Override
	protected String msgTrace() {
		return "creating references to classes";
	}

	/**
	 * Overriden to initialize some flags to <code>false</code>
	 * (e.g. at the begining of a .c file) 
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		super.visit(elt);

		inSizeofExpression = false;
		inCastExpression = false;
		inTemplateArgumentExpression = false;
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		if (! inCastExpression) {
			super.visit(node);
			return PROCESS_SKIP;
		}
		else {
			// this is something like a cast to a fonction pointer type
			// do not deal with the FunctionDeclarator, but handle its possible parameter type
			return PROCESS_CONTINUE;
		}
		
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
			Reference ref = referenceToType( resolver.referedType(node.getName()) );
			dico.addSourceAnchor(ref, filename, node.getFileLocation());
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTNamedTypeSpecifier node) {
		if (inSizeofExpression || inCastExpression || inTemplateArgumentExpression) {
			Reference ref = referenceToType( resolver.referedType(node.getName()) );
			dico.addSourceAnchor(ref, filename, node.getFileLocation());
		}

		return PROCESS_SKIP;
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



	protected Reference referenceToType(Type referedType) {
		Reference ref = dico.addFamixReference(getContext().topBehaviouralEntity(), referedType,  getContext().getLastReference());
		getContext().setLastReference(ref);
		return ref;
	}

}
