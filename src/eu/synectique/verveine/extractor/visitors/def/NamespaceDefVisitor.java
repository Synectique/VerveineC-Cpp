package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class NamespaceDefVisitor extends AbstractVisitor {

	public NamespaceDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	public int visit(IASTDeclaration node) {
		/* optimisation: pruning AST visit on any declaration
		 * (Namespace declarations have their own visit method in CDT's ASTVisitor)
		 */
		return PROCESS_SKIP;
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		Namespace fmx;
		nodeName = node.getName();
		nodeBnd = getBinding(nodeName);

		fmx = dico.ensureFamixNamespace(nodeBnd, nodeName.getLastName().toString(), (Namespace) this.context.top());
		fmx.setIsStub(false);

		this.context.push(fmx);

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		return super.leave(node);
	}

}