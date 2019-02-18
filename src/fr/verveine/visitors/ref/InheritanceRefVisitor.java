package fr.verveine.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.Type;
import fr.verveine.plugin.CDictionary;
import fr.verveine.visitors.AbstractVisitor;

public class InheritanceRefVisitor extends AbstractVisitor {

	private Inheritance lastInheritance;

	public InheritanceRefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	protected String msgTrace() {
		return "creating inheritance relationships";
	}

	/*
	 * Recovering the sub-class
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class subClass = null;
		// compute nodeName and binding
		super.visit(node);
		subClass = dico.getEntityByKey(Class.class, nodeBnd);

		getContext().push(subClass);
		lastInheritance = null;

		for (ICPPASTBaseSpecifier base : node.getBaseSpecifiers()) {
			base.accept(this);
		}

		this.getContext().pop();

		return PROCESS_SKIP;
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		Class subClass = (Class) getContext().top();
		Type supClass = null;

		// why isn't it an IASTName like everywhere else?
		ICPPASTNameSpecifier baseName = node.getNameSpecifier(); 
		
		nodeBnd = baseName.resolveBinding();
		if ( (nodeBnd == null) || (nodeBnd instanceof IProblemBinding) ) {
			nodeBnd = resolver.mkStubKey((IASTName)baseName, Class.class);
		}
		supClass = dico.getEntityByKey(Type.class, nodeBnd);

		if (supClass == null) {
			supClass = (Class) resolver.resolveOrCreate( baseName.toString(), /*mayBeNull*/false, /*asType*/Class.class);
		}

		if (supClass != null) { 
			lastInheritance = dico.ensureFamixInheritance(supClass, subClass, lastInheritance);
		}

		return PROCESS_SKIP;
	}


}
