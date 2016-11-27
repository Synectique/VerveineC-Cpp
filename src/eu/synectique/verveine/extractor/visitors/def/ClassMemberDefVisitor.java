package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.Visibility;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public abstract class ClassMemberDefVisitor extends AbstractVisitor {

	protected Visibility currentVisibility;
	
	public ClassMemberDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	/**
	 * Overriden to initialize {@link #currentVisibility} to null
	 * (e.g. at the begining of a .c file) 
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		super.visit(elt);
		currentVisibility = null;
	}

	@Override
	protected int visit(ICPPASTVisibilityLabel node) {
		switch (node.getVisibility()) {
		case ICPPASTVisibilityLabel.v_private :   currentVisibility = Visibility.PRIVATE;   break;
		case ICPPASTVisibilityLabel.v_protected : currentVisibility = Visibility.PROTECTED; break;
		case ICPPASTVisibilityLabel.v_public :    currentVisibility = Visibility.PUBLIC;    break;
		}
		return PROCESS_CONTINUE;
	}

	/*
	 * Putting class definition on the context stack
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		currentVisibility = Visibility.PRIVATE;
		return visit((IASTCompositeTypeSpecifier)node);
	}
	
	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		currentVisibility = Visibility.PUBLIC;
		return visit((IASTCompositeTypeSpecifier)node);
	}

	@Override
	protected int visit(IASTCompositeTypeSpecifier node) {
		Class fmx;

		super.visit(node);
		fmx = (Class) dico.getEntityByKey(nodeBnd);

		this.getContext().push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = getContext().pop();

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTCastExpression node) {
		node.getOperand().accept(this);

		return PROCESS_SKIP;
	}

}
