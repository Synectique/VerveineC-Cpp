package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class NamespaceDefVisitor extends AbstractVisitor {

	public NamespaceDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	@Override
	public int visit(IASTDeclaration node) {
		/* Pruning AST visit on any declaration
		 * (Namespace declarations have their own visit method in CDT's ASTVisitor)
		 */
		return PROCESS_SKIP;
	}

	protected String msgTrace() {
		return "creating namespaces";
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		Namespace fmx;
		nodeName = node.getName();
		if (! nodeName.toString().equals("")) {
			nodeBnd = resolver.getBinding(nodeName);

			fmx = dico.ensureFamixNamespace(nodeBnd, nodeName.toString(), (Namespace) this.getContext().top());
			fmx.setIsStub(false);

			this.getContext().push(fmx);
		}

		for (IASTDeclaration decl : node.getDeclarations()) {
			decl.accept(this);
		}

		if (! nodeName.toString().equals("")) {
			getContext().pop();
		}

		return PROCESS_SKIP;
	}

}