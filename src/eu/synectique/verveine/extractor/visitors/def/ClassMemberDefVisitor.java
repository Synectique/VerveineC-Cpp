package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.Visibility;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class ClassMemberDefVisitor extends AbstractVisitor {

	protected Visibility currentVisibility;

	public ClassMemberDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	protected int visit(ICPPASTVisibilityLabel node) {
		switch (node.getVisibility()) {
		case ICPPASTVisibilityLabel.v_private :   currentVisibility = Visibility.PRIVATE;   break;
		case ICPPASTVisibilityLabel.v_protected : currentVisibility = Visibility.PROTECTED; break;
		case ICPPASTVisibilityLabel.v_public :    currentVisibility = Visibility.PUBLIC;    break;
		}
		return PROCESS_CONTINUE;
	}

	/*
	 * Visiting a class definition, need to put it on the context stack to create its members
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

	/*
	 * To avoid getting ICPPASTDeclarator of types passed as possible attributes or methods
	 */
	@Override
	protected int visit(IASTSimpleDeclaration node) {
		if (declarationIsTypedef(node)) {
			return PROCESS_SKIP;
		}
		return PROCESS_CONTINUE;
	}

}
