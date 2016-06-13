package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;

import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.extractor.utils.StubBinding;


public class SignatureBuilderVisitor extends AbstractVisitor {

	protected String signature;
	
	public String getSignature() {
		return signature;
	}

	public SignatureBuilderVisitor(CDictionary dico) {
		super(dico, null);
	}
	
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		signature = simpleName(node.getName().toString()) + "(";
		boolean first = true;
		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			if (! first) {
				signature += ",";
			}
			param.accept(this);
			first = false;
		}
		signature += ")";
		return PROCESS_SKIP;
	}
	
	/*
	 * superclass visit(IASTParameterDeclaration node) tries to get a binding that we don't need here
	 * so overrides the methods
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	@Override
	public int visit(ICPPASTDeclarator node) {
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
	public int visit(ICPPASTNamedTypeSpecifier node) {
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
