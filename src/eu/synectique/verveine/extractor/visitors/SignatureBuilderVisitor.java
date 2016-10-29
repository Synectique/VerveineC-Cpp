package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;

import eu.synectique.verveine.extractor.plugin.CDictionary;

/**
 * A visitor specialized in reconstructing the signature of a method/function.
 * It works by visiting the node itself to get the name and parameters, and visiting the DeclSpecifier of the node's parent
 * to get the declared type.
 * The so called "FullSignature" is a concatenation of both
 * 
 * Based on the inheritance hierarchy of functions (see below), the visitor has 2 main entry points:
 * <ul>
 * <li>visit(IASTStandardFunctionDeclarator)</li>
 * <li>visit(ICASTKnRFunctionDeclarator)</li>
 * </ul>
 *   
 * there are also 2 other "parent entry points" used to get the return type of the function
 * <ul>
 * <li>visit(IASTStandardFunctionDeclarator)</li>
 * <li>visit(ICASTKnRFunctionDeclarator)</li>
 * </ul>
 *   
 * For reference, the inheritance hierarchy of function declarator nodes:
 * <ul>
 * <li> IASTFunctionDeclarator
 *   <ul>
 *   <li> IASTStandardFunctionDeclarator
 *     <ul>
 *     <li> ICPPASTFunctionDeclarator
 *       <ul>
 *       <li> ICPPASTFunctionTryBlockDeclarator</li>
 *       </ul>
 *     </li>
 *     </ul>
 *   </li>
 *   <li>ICASTKnRFunctionDeclarator</li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * @author anquetil
 */
public class SignatureBuilderVisitor extends AbstractVisitor {

	protected String signature;

	public SignatureBuilderVisitor(CDictionary dico) {
		super(dico, null, null);
		signature = "";
	}

	protected String msgTrace() {
		return null;
	}

	public String getSignature() {
		return signature;
	}

	//  MAIN ENTRY POINTS --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTStandardFunctionDeclarator node) {
		// name
		signature = unqualifiedName(node.getName().toString());
		// parameters
		visitParameters(node.getParameters());
		// return type
		signature += "->";
		node.getParent().accept(this);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICASTKnRFunctionDeclarator node) {
		// name
		signature = unqualifiedName(node.getName().toString());
		// parameters
		visitParameters(node.getParameterDeclarations());
		// return type
		signature += "->";
		node.getParent().accept(this);

		return PROCESS_SKIP;
	}

	//  "PARENT ENTRY POINTS" --------------------------------------------------------------------------------------------


	@Override
	public int visit(IASTSimpleDeclaration parent) {
		parent.getDeclSpecifier().accept(this);

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTFunctionDefinition parent) {
		parent.getDeclSpecifier().accept(this);

		return PROCESS_SKIP;
	}

	// OTHER VISIT METHOD --------------------------------------------------------------------------------------------

	/**
	 * Similar to {@link #AbstractVisitor.visitParameters(IASTNode[], eu.synectique.verveine.core.gen.famix.BehaviouralEntity)}
	 * But does not need the famix entity.
	 * Not worth it using the above method
	 */
	protected void visitParameters(IASTNode[] params) {
		signature += "(";
		boolean first = true;
		for (IASTNode param : params) {
			if (! first) {
				signature += ",";
			}
			param.accept(this);
			first = false;
		}
		signature += ")";
	}
	
	/*
	 * superclass visit(IASTParameterDeclaration node) tries to get a binding that we don't need here
	 * so overrides the methods
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visitInternal(IASTDeclarator node) {
		for (@SuppressWarnings("unused") IASTPointerOperator ptr : node.getPointerOperators()) {
			signature += "*";
		}
		return PROCESS_CONTINUE;
	}

	@Override
	public int visit(ICASTCompositeTypeSpecifier node) {
		signature += node.getName();
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTElaboratedTypeSpecifier node) {
		signature += node.getName();
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTEnumerationSpecifier node) {
		signature += node.getName();
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTNamedTypeSpecifier node) {
		signature += node.getName();
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTSimpleDeclSpecifier node) {
		String tname = dico.primitiveTypeName(node.getType());
		if (tname != null) {
			signature += tname;
		}
		return PROCESS_SKIP;
	}

}
