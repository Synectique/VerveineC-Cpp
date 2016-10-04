package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.Visibility;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public abstract class ClassMemberDefVisitor extends AbstractVisitor {

	protected Visibility currentVisibility;

	public ClassMemberDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
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
	 * Putting class definition on the context stack
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		return visit((IASTCompositeTypeSpecifier)node);
	}
	
	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		return visit((IASTCompositeTypeSpecifier)node);
	}

	@Override
	protected int visit(IASTCompositeTypeSpecifier node) {
		Class fmx;

		super.visit(node);
		fmx = (Class) dico.getEntityByKey(nodeBnd);

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

}
