package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class AttributeDefVisistor extends ClassMemberDefVisitor {

	public AttributeDefVisistor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	/*
	 * To avoid type name with "parameter" as in: aType<aParam>
	 */
	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	/*
	 * Inheritance declaration can lead to ICPPASTDeclarator
	 */
	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		return PROCESS_SKIP;
	}

	/*
	 * to avoid parameter or local variable declarations
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		return PROCESS_SKIP;
	}

	/*
	 * to avoid parameter or local variable declarations
	 */
	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		return PROCESS_SKIP;
	}

	/*
	 * to avoid parameter or local variable declarations
	 */
	@Override
	protected int visit(IASTFunctionDeclarator node) {
		return PROCESS_SKIP;
	}

	/*
	 * to avoid parameter or local variable declarations
	 */
	@Override
	protected int visit(IASTFunctionDefinition node) {
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTParameterDeclaration parameterDeclaration) {
		// prunes parameter types in templates (they are also ICPPASTDeclarator)
		return PROCESS_SKIP;
	}

	/*
	 * We should only get here in the case of an attribute declaration.
	 */
	@Override
	protected int visit(ICPPASTDeclarator node) {
		Attribute fmx = null;

		nodeName = node.getName();
		nodeBnd = getBinding(nodeName);
		if (nodeBnd == null) {
			nodeBnd = StubBinding.getInstance(Attribute.class, dico.mooseName(context.topType(), nodeName.toString()));
		}

		fmx = dico.ensureFamixAttribute(nodeBnd, nodeName.toString(), context.topType());
		fmx.setIsStub(false);

		/* For attributes (ICPPASTDeclarator) the location is that of the parent AST node, i.e. the declaration
		 * For example, in "int a,b;" the declaration starts at "int" whereas there are 2 declarators: a and b
		 */
		dico.addSourceAnchor(fmx, filename, node.getParent().getFileLocation());

		return PROCESS_SKIP;
	}

}
