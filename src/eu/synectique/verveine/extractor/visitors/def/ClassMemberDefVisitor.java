package eu.synectique.verveine.extractor.visitors.def;

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

	/*
	 * Visiting a class definition, need to put it on the context stack to create its members
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;

		// compute nodeName and binding
		super.visit(node);

		fmx = (Class) dico.getEntityByKey(nodeBnd);
		
		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTVisibilityLabel node) {
		switch (node.getVisibility()) {
		case ICPPASTVisibilityLabel.v_private :   currentVisibility = Visibility.PRIVATE;   break;
		case ICPPASTVisibilityLabel.v_protected : currentVisibility = Visibility.PROTECTED; break;
		case ICPPASTVisibilityLabel.v_public :    currentVisibility = Visibility.PUBLIC;    break;
		}
		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();
		return PROCESS_CONTINUE;		
	}

}
