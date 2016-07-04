package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;

/**
 * A visitor specialized in reconstructing the signature of a method/function.
 * It works by visiting the node itself to get the name and parameters, and visiting the DeclSpecifier of the node's parent
 * to get the declared type.
 * The so called "FullSignature" is a concatenation of both
 * @author anquetil
 */
public class SignatureBuilderVisitor extends AbstractVisitor {

	protected String signature;
	
	protected boolean visitSignature;

	public SignatureBuilderVisitor(CDictionary dico) {
		super(dico, null);
	}

	public String getFullSignature(ICPPASTFunctionDeclarator node) {
		String fullSignature;

		signature = "";
		node.accept(this);
		fullSignature = signature;
		
		signature = "";
		if (node.getParent() instanceof IASTSimpleDeclaration) {
			((IASTSimpleDeclaration)node.getParent()).getDeclSpecifier().accept(this);
		}
		else if (node.getParent() instanceof IASTFunctionDefinition) {
			((IASTFunctionDefinition)node.getParent()).getDeclSpecifier().accept(this);
		}
		// else ???
		fullSignature += ":" + signature;

		return fullSignature;
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
