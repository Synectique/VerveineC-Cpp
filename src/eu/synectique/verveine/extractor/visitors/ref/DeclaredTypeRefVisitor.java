package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;

public class DeclaredTypeRefVisitor extends AbstractRefVisitor {

	/**
	 * The referred type set in visit(ICPPASTFunctionDefinition) or visit(IASTSimpleDeclaration)
	 * used in visit(ICPPASTFunctionDeclarator) for methods or visit(ICPPASTDeclarator) for attribute
	 */
	private Type referredType;

	public DeclaredTypeRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	protected String msgTrace() {
		return "recording variables declared type and methods/functions return type";
	}

	/*
	 * prune to not visit template parameters
	 */
	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		node.getDeclaration().accept(this);
		return PROCESS_SKIP;
	}

	/*
	 * get the function declarator, the return type and pass to visit(ICPPASTFunctionDeclarator)
	 */
	@Override
	protected int visit(IASTFunctionDefinition node) {
		returnedEntity = null;
		node.getDeclSpecifier().accept(this);
		referredType = (Type)returnedEntity;

		node.getDeclarator().accept(this);

		referredType = null;

		return PROCESS_SKIP;
	}

	/*
	 * class members: attribute/methods
	 */
	@Override
	protected int visit(IASTSimpleDeclaration node) {
		if (declarationIsTypedef(node)) {
			return PROCESS_SKIP;
		}

		returnedEntity = null;
		node.getDeclSpecifier().accept(this);
		referredType = (Type)returnedEntity;

		for (IASTDeclarator declarator : node.getDeclarators()) {
			// gets the entity and sets its simpleDeclaratorReferredType
			this.visit( declarator );
		}
		referredType = null;

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
			nodeBnd = mkStubKey(nodeName, Attribute.class);
		}

		fmx = (Attribute) dico.getEntityByKey(nodeBnd);
		fmx.setDeclaredType(referredType);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTStandardFunctionDeclarator node) {
		BehaviouralEntity fmx;
		// compute nodeName and binding
		super.visit(node);

		fmx = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);
		// try harder
		if (fmx == null) {
			fmx = resolveBehaviouralFromName(node, nodeBnd);
		}

		// set the declared type
		if ( (! isConstructor((BehaviouralEntity) fmx)) && (! isDestructor((BehaviouralEntity) fmx)) ) {
			((BehaviouralEntity)fmx).setDeclaredType(referredType);
		}

		this.context.push(fmx);

		// visit parameters to set their declared type
		for (IASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		returnedEntity = this.context.pop();

		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		Parameter fmx = null;

		// get the parameter
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}
		fmx = (Parameter) dico.getEntityByKey(nodeBnd);

		/*
		if (fmx == null) {
			String paramName = null;
			BehaviouralEntity parent = null;
			// get param name and parent
			parent = context.topBehaviouralEntity();
			paramName = nodeName.toString();

			// last try to recover parameter
			fmx = (Parameter) findInParent(paramName, parent, /*recursive* /false);
		}
		 */

		// now get the declared type
		if (fmx != null) {
			if (node.getParent() instanceof ICPPASTTemplateDeclaration) {
				// parameterType in a template
				// ignore for now
			}
			else {
				node.getDeclSpecifier().accept(this);
				fmx.setDeclaredType( (Type) returnedEntity );
			}
		}
		returnedEntity = fmx;

		return PROCESS_SKIP;
	}

}
