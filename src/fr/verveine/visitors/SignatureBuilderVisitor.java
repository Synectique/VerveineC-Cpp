package fr.verveine.visitors;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;

import fr.verveine.plugin.CDictionary;
import fr.verveine.utils.QualifiedName;
import fr.verveine.utils.Trace;

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

	public static final String CPP_FCT_RETURN_SEPARATOR = "->";

	protected String signature;

	public SignatureBuilderVisitor() {
		super(/*dico*/null, /*IIndex*/null, /*rootFolder*/null);
		signature = "";
	}

	protected String msgTrace() {
		return null;
	}

	public String getSignature() {
		return signature;
	}

	static public String signatureFromAST(IASTFunctionDeclarator node) {
		// for behavioral, we put the full signature in the key to have better chance of recovering it
		SignatureBuilderVisitor sigVisitor = new SignatureBuilderVisitor();
		node.accept(sigVisitor);

		return sigVisitor.getSignature();
	}

	//  MAIN ENTRY POINTS --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTStandardFunctionDeclarator node) {
		// name

		if (node.getName() != null) {
			signature = new QualifiedName(node.getName()).unqualifiedName();
			// parameters
			visitParameters(node.getParameters());
			// return type
			visitParent(node.getParent());
		}
		else {
			signature = null;
		}
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICASTKnRFunctionDeclarator node) {
		// name
		signature = new QualifiedName(node.getName()).unqualifiedName();
		// parameters
		visitParameters(node.getParameterDeclarations());
		// return type
		visitParent(node.getParent());

		return PROCESS_SKIP;
	}

	/**
	 * This method could be more elegant ( "instanceof" replaced by direct call to "parent.accept(this)" ), but it protects against infinite recursion:
	 * here we visit the parent which could cause the FuctionDeclaration node to be visited again ...
	 * 
	 * In this method, we remove this risk by allowing only those parents that we now we can deal with
	 */
	protected void visitParent(IASTNode parent) {
		signature += CPP_FCT_RETURN_SEPARATOR;
		
		if (parent instanceof IASTSimpleDeclaration) {
			((IASTSimpleDeclaration)parent).getDeclSpecifier().accept(this);
		}
		else if (parent instanceof IASTFunctionDefinition) {
			((IASTFunctionDefinition)parent).getDeclSpecifier().accept(this);
		}
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
/*
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

	/*
	 * Visiting the return type of the behavioural
	 * To help in Symbol resolution, its best if the return type is not qualified
	 * /
	@Override
	public int visit(IASTNamedTypeSpecifier node) {
		signature += new QualifiedName(node.getName()).unqualifiedName();
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTSimpleDeclSpecifier node) {
		String tname = CDictionary.primitiveTypeName(node.getType());
		if (tname != null) {
			signature += tname;
		}
		return PROCESS_SKIP;
	}
*/

@Override
public int visit(IASTDeclSpecifier node) {
	signature += node.getRawSignature();

	return PROCESS_SKIP;
}

}
