package eu.synectique.verveine.extractor.cpp;

import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTAttribute;
import org.eclipse.cdt.core.dom.ast.IASTAttributeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTToken;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCapture;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTClassVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDecltypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVirtSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.internal.core.dom.parser.ASTAmbiguousNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTElaboratedTypeSpecifier;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNamedTypeSpecifier;

public class MainVisitor extends VerveineVisitor {

	public MainVisitor(Map<IBinding,IASTName> dico) {
		super(dico);
	}

	@Override
	public int visit(IASTTranslationUnit node) {
		traceup("IASTTranslationUnit: "+node.getFilePath());
		
		return super.visit(node);
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
//		traceup("ICPPASTNamespaceDefinition: "+node.getName());
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
//		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTArrayModifier node) {
//		traceup("TRACE, Visiting IASTArrayModifier ");
		return super.visit(node);
	}

	@Override
	public int visit(IASTDeclarator node) {
		traceup("IASTDeclarator:");
		IASTName nodeName = node.getName();
		tracename(nodeName);
		nodeName.resolveBinding();
		tracemsg("       /// and after resolveBinding() ...");
		tracename(nodeName);

		if (isBound(nodeName)) {
			dico.put(nodeName.getBinding(), nodeName);
		}

		if (node instanceof IASTFunctionDeclarator) {
			this.visit((IASTFunctionDeclarator)node);
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclarator node) {
		if (node instanceof IASTFunctionDeclarator) {
			this.leave((IASTFunctionDeclarator)node);
		}

		tracedown("IASTDeclarator: ");
		tracename(node.getName());
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
		String trace=node.getRawSignature();
		int cr = trace.indexOf('\n');
		traceup("IASTDeclSpecifier: "+(cr<0?trace:trace.substring(0, cr)+"..."));

		if (node instanceof CPPASTElaboratedTypeSpecifier) {
			// -> struct/class)
		}
		else if (node instanceof CPPASTNamedTypeSpecifier) {
			// -> struct/class)
		}
		else if (node instanceof IASTCompositeTypeSpecifier) {
			this.visit( (IASTCompositeTypeSpecifier)node );
		}
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclSpecifier node) {
		tracedown();
	
		return super.leave(node);
	}

	@Override
	public int visit(IASTEnumerator node) {
//		traceup("IASTEnumerator ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTEnumerator node) {
//		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTExpression node) {
		tracemsg("IASTExpression ("+node.getClass().getSimpleName()+")");
		if (node instanceof IASTFunctionCallExpression) {
			traceup("FunctionCall name:");
			visit( ((IASTFunctionCallExpression)node).getFunctionNameExpression() );
			tracedown();
			return ASTVisitor.PROCESS_SKIP;  // because we already visited the FunctionNameExpression
		}
		else if (node instanceof IASTIdExpression) {
			IASTName idName = ((IASTIdExpression) node).getName();
			tracemsg("    ->  IdExpression:");
			tracename(idName);
			traceanchor(idName.getImageLocation());
		}
		else if (node instanceof IASTFieldReference) {
			tracemsg("    ->  FieldReference:");
			tracename( ((IASTFieldReference)node).getFieldName() );
			traceup("Field owner:");
			visit( ((IASTFieldReference)node).getFieldOwner() );
			tracedown();
		}
		return super.visit(node);
	}

	@Override
	public int leave(IASTExpression node) {
		// never called
		return super.leave(node);
	}

	@Override
	public int visit(IASTInitializer node) {
//		traceup("IASTInitializer ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTInitializer node) {
//		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		tracemsg("IASTParameterDeclaration: ");
		IASTDeclarator decl = node.getDeclarator();
		if (decl != null) {
			this.tracename(decl.getName());
		}
		else {
			tracemsg("    -> null declarator");
		}

		return PROCESS_SKIP;
	}

	@Override
 	public int leave(IASTParameterDeclaration node) {
		// never called
		return super.leave(node);
	}

	@Override
	public int visit(IASTPointerOperator node) {
		traceup("IASTPointerOperator ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTPointerOperator node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTProblem node) {
		traceup("IASTProblem ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTProblem node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTStatement node) {
		traceup("IASTStatement ("+node.getClass().getSimpleName()+")");
		return super.visit(node);
	}

	@Override
	public int leave(IASTStatement node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTTypeId node) {
		traceup("IASTTypeId ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTTypeId node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICASTDesignator node) {
		traceup("ICASTDesignator ");
		return super.visit(node);
	}

	@Override
	public int leave(ICASTDesignator node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTBaseSpecifier node) {
		traceup("ICPPASTBaseSpecifier:");
		tracename(node.getName());
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTBaseSpecifier node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTCapture node) {
		traceup("ICPPASTCapture ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTCapture node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTTemplateParameter node) {
		traceup("ICPPASTTemplateParameter ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTTemplateParameter node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ASTAmbiguousNode node) {
		tracemsg("ASTAmbiguousNode ");
		return super.visit(node);
	}

	@Override
	public int visit(IASTAttribute node) {
		traceup("IASTAttribute: "+node.getName());
		return super.visit(node);
	}

	@Override
	public int leave(IASTAttribute node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTAttributeSpecifier node) {
		traceup("IASTAttributeSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTAttributeSpecifier node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTComment node) {
		traceup("IASTComment "+node.toString());
		return super.visit(node);
	}

	@Override
	public int leave(IASTComment node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(IASTToken node) {
		traceup("IASTToken type="+node.getTokenType());
		return super.visit(node);
	}

	@Override
	public int leave(IASTToken node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTClassVirtSpecifier node) {
		traceup("ICPPASTClassVirtSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTClassVirtSpecifier node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTDecltypeSpecifier node) {
		traceup("ICPPASTDecltypeSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTDecltypeSpecifier node) {
		tracedown();
		return super.leave(node);
	}

	@Override
	public int visit(ICPPASTVirtSpecifier node) {
		traceup("ICPPASTVirtSpecifier ");
		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTVirtSpecifier node) {
		tracedown();
		return super.leave(node);
	}

	// ADDITIONAL VISITING METODS

	public void visit(IASTCompositeTypeSpecifier node) {
		IASTName nodeName = ((IASTCompositeTypeSpecifier)node).getName();
		IBinding bnd = nodeName.resolveBinding();

		if ( (bnd != null) && (bnd instanceof ICPPClassType) ) {
			tracemsg("    -> IASTCompositeTypeSpecifier (and a class)");

		}
	}

	public int visit(CPPASTFunctionDefinition node) {
		traceup("CPPASTFunctionDefinition ");
		return PROCESS_SKIP;
	
	}
	
	public void visit(IASTFunctionDeclarator node) {
		tracemsg("    -> IASTFunctionDeclarator");

		IASTFunctionDeclarator func = (IASTFunctionDeclarator)node;
		IASTName nodeName = func.getName();
		if (nodeName != null) {
			IBinding bnd = nodeName.resolveBinding();

			if (bnd != null) {
				boolean iscpp = (bnd instanceof ICPPMethod);
			}
		}
		else {
			//TODO ANONYMOUS function
		}
	}
	
	protected void leave(IASTFunctionDeclarator node) {
		try {
			IASTName nodeName = null;
		}
		catch (Exception e) {
			// ignore
		}
	}

}
