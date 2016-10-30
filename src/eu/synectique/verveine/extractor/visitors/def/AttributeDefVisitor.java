package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.Enum;
import eu.synectique.verveine.core.gen.famix.EnumValue;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class AttributeDefVisitor extends ClassMemberDefVisitor {

	public AttributeDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	protected String msgTrace() {
		return "creating attributes and struct members";
	}

	/*
	 * To avoid type name with "parameter" as in: aType<aParam>
	 */
	@Override
	protected int visit(IASTNamedTypeSpecifier node) {
		return PROCESS_SKIP;
	}

	/*
	 * Prune inheritance declaration
	 */
	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		return PROCESS_SKIP;
	}

	/*
	 * to avoid parameter or local variable declarations
	 * May miss anonymous class definition ... (but unlikely to have attributes)
	 */
	@Override
	protected int visit(IASTFunctionDeclarator node) {
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
	protected int visit(IASTFunctionDefinition node) {
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTParameterDeclaration parameterDeclaration) {
		// prunes parameter types in templates
		return PROCESS_SKIP;
	}

	/*
	 * We should only get here in the case of an attribute declaration.
	 */
	@Override
	public int visitInternal(IASTDeclarator node) {
		Attribute fmx = null;

		nodeName = node.getName();
		nodeBnd = getBinding(nodeName);
		if (nodeBnd == null) {
			nodeBnd = mkStubKey(nodeName, Attribute.class);
		}

		fmx = dico.ensureFamixAttribute(nodeBnd, nodeName.toString(), context.topType());
		fmx.setIsStub(false);

		/* For attributes (ICPPASTDeclarator) the location is that of the parent AST node, i.e. the declaration
		 * For example, in "int a,b;" the declaration starts at "int" whereas there are 2 declarators: a and b
		 */
		dico.addSourceAnchor(fmx, filename, node.getParent().getFileLocation());

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTSimpleDeclaration node) {

		if (declarationIsTypedef(node)) {
			node.getDeclSpecifier().accept(this);
			// skip declarators
			return PROCESS_SKIP;
		}

		return PROCESS_CONTINUE;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
		nodeBnd = null;
		nodeName = node.getName();

		if (nodeName.equals("")) {
			nodeBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Enum.class, dico.mooseName(context.topBehaviouralEntity(), ""+node.getFileLocation().getNodeOffset()));
		}
		else {
			nodeBnd = getBinding(nodeName);
			if (nodeBnd == null) {
				nodeBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Enum.class, dico.mooseName(context.topBehaviouralEntity(), nodeName.toString()));
			}
		}

		this.context.push(dico.getEntityByKey(nodeBnd));
		for (IASTEnumerator decl : node.getEnumerators()) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTEnumerator node) {
		EnumValue fmx;

		nodeBnd = null;
		nodeName = node.getName();
		if (nodeBnd == null) {
			nodeBnd = StubBinding.getInstance(EnumValue.class, dico.mooseName(context.topBehaviouralEntity(), nodeName.toString()));
		}
		fmx = dico.ensureFamixEnumValue(nodeBnd, nodeName.toString(), (Enum)context.top(), /*persistIt*/true);
		dico.addSourceAnchor(fmx, filename, node.getFileLocation());

		return PROCESS_SKIP;
	}

}
