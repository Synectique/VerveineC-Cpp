package eu.synectique.verveine.extractor.ref;

import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;

import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.def.CDictionaryDef;

/** Specialized visitor for parameter declaration of a FunctionDeclarator
 * This simplifies visiting IASTDeclSpecifier (type of parameters) and IASTDeclarator (parameter itself)
 * @author anquetil
 */
public class ParamDeclVisitor extends AbstractRefVisitor {

	/** the FamixMethod for which we are visiting the IASTParameterDeclaration
	 */
	private Method fmxMth;

	/** (Famix)Type of the parameter
	 */
	private Type paramType;

	/** Name of the parameter
	 */
	private String name;

	/** constructor, receives the FamixMethod for which we are visiting the IASTParameterDeclaration 
	 */
	public ParamDeclVisitor(CDictionaryRef dico, Method fmx) {
		super(dico);
		this.fmxMth = fmx;
	}

	// VISITING METHODS

	
	/*
	 * 
			// creating the method's parameters
			// unless classSummary is true in which case we might need to create References between classes
			List<VariableDeclaration> paramAsVarList;
			for (SingleVariableDeclaration param : (List<SingleVariableDeclaration>)node.parameters()) {
				// Note: method and ParamTyp bindings are null for ParameterType :-(
				paramAsVarList = new ArrayList<VariableDeclaration>(1);
				paramAsVarList.add(param);

				eu.synectique.verveine.core.gen.famix.Type varTyp = referedType(param.getType(), fmx, false);
				visitVariablesDeclarations(node, varTyp, paramAsVarList, fmx);
			}
*/	
	
	@Override
	public int visit(IASTParameterDeclaration node) {
		tracer.up("### IASTParameterDeclaration ");
		return super.visit(node);
	}

	@Override
	public int leave(IASTParameterDeclaration node) {
//		dico.ensureFamixParameter((IBinding)null, name, paramType, fmxMth, /*persistIt*/true);
		return super.visit(node);
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int visit(ICPPASTDeclarator node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(IASTFunctionDeclarator node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(ICPPASTDeclarator node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(ICASTCompositeTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(IASTEnumerationSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int leave(ICPPASTNamedTypeSpecifier node) {
		// TODO Auto-generated method stub
		return 0;
	}

}
