package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.visitors.CDictionary;

/** Specialized visitor for parameter declaration of a FunctionDeclarator
 * This simplifies visiting IASTDeclSpecifier (type of parameters) and IASTDeclarator (parameter itself)
 * @author anquetil
 */
public class ParamDeclVisitor extends AbstractRefVisitor {

	/* 
	 * name of the parameter to define
	 */
	private Parameter fmxParam;

	/*
	 * Type of the parameter
	 */
	private Type fmxType;

	public ParamDeclVisitor(CDictionary dico, IIndex index, EntityStack context) {
		super(dico, index, context, /*visitNodes*/true);

		tracer = new NullTracer("PDV>");
	}

	// VISITING METHODS ==================================================================================================

	@Override
	public int visit(IASTParameterDeclaration node) {
		visit(node.getDeclarator()); 	// parameter
		visit(node.getDeclSpecifier()); // type
		
		if (fmxParam != null) {
			fmxParam.setDeclaredType(fmxType);
		}
		
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTDeclarator node) {
		IASTName nodeName = node.getName();
		IIndexBinding bnd = null;

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd != null) {
			fmxParam = (Parameter) dico.getEntityByKey(bnd);
		}

		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNamedTypeSpecifier node) {
		IASTName nodeName = node.getName();
		IIndexBinding bnd = null;

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd != null) {
			fmxType = (Type) dico.getEntityByKey(bnd);
		}

		if (fmxType == null) {
			fmxType = dico.ensureFamixType(/*key*/null, nodeName.toString(), /*owner*/null, /*persistIt*/true);
		}

		return PROCESS_CONTINUE;
	}

}
