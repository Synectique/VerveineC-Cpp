package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFieldDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;

public class DeclaredTypeRefVisitor extends AbstractRefVisitor {

	/**
	 * The referred type set in visit(SimpleDeclaration),
	 * used in visit(ICPPASTFunctionDeclarator) for methods or visit(ICPPASTDeclarator) for attribute
	 */
	private Type simpleDeclaratorReferredType;
	
	public DeclaredTypeRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	protected String msgTrace() {
		return "recording variables declared types and methods/functions return types";
	}

	/*
	 * putting class definition on the context stack
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;

		/* Gets the key (IBinding) of the node to recover the famix type entity */
		super.visit(node);

		fmx = (Class) dico.getEntityByKey(nodeBnd);

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

	/*
	 * visit declaration and that's it
	 */
	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		returnedEntity = null;
		node.getDeclSpecifier().accept(this);
		simpleDeclaratorReferredType = (Type)returnedEntity;

		this.visit( (ICPPASTFunctionDeclarator)node.getDeclarator());

		simpleDeclaratorReferredType = null;

		return PROCESS_SKIP;
	}

	 /*
	  * redefined to not visit template parameters
	  */
	 @Override
	 protected int visit(ICPPASTTemplateDeclaration node) {
		 node.getDeclaration().accept(this);
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
		simpleDeclaratorReferredType = (Type)returnedEntity;

		for (IASTDeclarator declarator : node.getDeclarators()) {
			// gets the entity and sets its simpleDeclaratorReferredType
			this.visit( declarator );
		}
		simpleDeclaratorReferredType = null;

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
		fmx.setDeclaredType(simpleDeclaratorReferredType);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
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
			((BehaviouralEntity)fmx).setDeclaredType(simpleDeclaratorReferredType);
		}

		this.context.push(fmx);

		// visit parameters to set their declared type
		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		returnedEntity = this.context.pop();

		return PROCESS_SKIP;
	}
	
}
