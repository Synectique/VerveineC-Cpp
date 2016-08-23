package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class InheritanceRefVisitor extends AbstractVisitor {

	private Inheritance lastInheritance;

	public InheritanceRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	protected String msgTrace() {
		return "recording inheritance relationships";
	}

	/*
	 * Recovering the sub-class
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class subClass = null;
		// compute nodeName and binding
		super.visit(node);
		subClass = (Class) dico.getEntityByKey(nodeBnd);

		context.push(subClass);
		lastInheritance = null;

		for (ICPPASTBaseSpecifier base : node.getBaseSpecifiers()) {
			base.accept(this);
		}

		this.context.pop();

		return PROCESS_SKIP;
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		Class subClass = (Class) context.top();
		Type supClass = null;

		// why isn't it an IASTName like everywhere else?
		ICPPASTNameSpecifier baseName = node.getNameSpecifier(); 
		
		nodeBnd = baseName.resolveBinding();
		if ( (nodeBnd == null) || (nodeBnd instanceof IProblemBinding) ) {
			nodeBnd = mkStubKey((IASTName)baseName, Class.class);
		}
		supClass = (Type) dico.getEntityByKey(nodeBnd);

		if (supClass == null) {
			supClass = resolveOrClass( baseName.toString());
		}

		if (supClass != null) { 
			lastInheritance = dico.ensureFamixInheritance(supClass, subClass, lastInheritance);
		}

		return PROCESS_SKIP;
	}

}
