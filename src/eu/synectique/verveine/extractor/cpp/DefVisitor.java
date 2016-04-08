package eu.synectique.verveine.extractor.cpp;

import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.IEnumeration;
import org.eclipse.cdt.core.model.IField;
import org.eclipse.cdt.core.model.IFunction;
import org.eclipse.cdt.core.model.IFunctionDeclaration;
import org.eclipse.cdt.core.model.IMethod;
import org.eclipse.cdt.core.model.IMethodDeclaration;
import org.eclipse.cdt.core.model.INamespace;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.IStructure;
import org.eclipse.cdt.core.model.IStructureDeclaration;
import org.eclipse.cdt.core.model.ITypeDef;
import org.eclipse.cdt.core.model.IVariable;
import org.eclipse.cdt.core.model.IVariableDeclaration;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.Tracer;

/**
 * Not a Visitor  on an AST, visits the tree of ICElement and defines all entities
 * These entities will be needed by the RefVisitor
 */
public class DefVisitor implements ICElementVisitor {

	/** 
	 * The dictionary that creates FAMIX Entities and hold the FAMIX repository
	 */
	protected CDictionary dico;

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack2 context;

	protected Tracer tracer = new Tracer();

	public DefVisitor(CDictionary dico) {
	    this.dico = dico;
		this.context = new EntityStack2();
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ======================================================================================================
	
	@Override
	public boolean visit(ICElement elt) {
		tracer.up("visit_ICElement:"+elt.getElementName());
		switch (elt.getElementType()) {
		case ICElement.C_PROJECT:
		case ICElement.C_CCONTAINER:
		case ICElement.C_UNIT:
			return true; // visit children and we don't care about knowing when these visit ends

		case ICElement.C_NAMESPACE:
			visit( (INamespace) elt);
			break;
		case ICElement.C_ENUMERATION:
			visit( (IEnumeration) elt);
			break;
		case ICElement.C_CLASS_DECLARATION:
		case ICElement.C_STRUCT_DECLARATION:
			visit( (IStructureDeclaration) elt);
			break;
		case ICElement.C_CLASS:
		case ICElement.C_UNION:
		case ICElement.C_STRUCT:
			visit( (IStructure) elt);
			break;
		case ICElement.C_METHOD_DECLARATION:
			visit( (IMethodDeclaration) elt);
			break;
		case ICElement.C_METHOD:
			visit( (IMethod) elt);
			break;
		case ICElement.C_FUNCTION_DECLARATION:
			visit( (IFunctionDeclaration) elt);
			break;
		case ICElement.C_FUNCTION:
			visit( (IFunction) elt);
			break;
		case ICElement.C_FIELD:
			visit( (IField) elt);
			break;
		case ICElement.C_VARIABLE:
			visit( (IVariable) elt);
			break;
		case ICElement.C_VARIABLE_DECLARATION:
			visit( (IVariableDeclaration) elt);
			break;
		case ICElement.C_TYPEDEF:
			visit( (ITypeDef) elt);
			break;
		default:
			//  don't know what it is, don't know what to do with it
		}
		
		tracer.down("end visit_ICElement:"+elt.getElementName());
		/* 
		 * Return false in most cases so that we can handle the visiting of children of the nodes ourselves
		 * so that we can manage properly the context stack.
		 * 
		 * What's the use of CDT ICElementVisitor in this case? I am really not sure ...
		 */
		return false;
	}

	private void visit(INamespace elt) {
		Namespace fmx = dico.createNamespace(elt.getElementName(), (ScopingEntity) this.context.top());
		fmx.setIsStub(false);
		tracer.up("created Namespace:"+fmx.getName());

		this.context.push(fmx);
		visitChildren(elt);
		this.context.pop();
		tracer.down("done with Namespace:"+fmx.getName());
	}

	private void visit(IEnumeration elt) {
	}

	private void visit(IStructureDeclaration elt) {
	}

	private void visit(IStructure elt) {
		if (elt.getElementType() == ICElement.C_CLASS) {
			eu.synectique.verveine.core.gen.famix.Class fmx = dico.createClass(elt.getElementName(), (ContainerEntity)context.top());
			fmx.setIsStub(false);
			tracer.up("created Class:"+fmx.getName());

			this.context.push(fmx);
			visitChildren(elt);
			this.context.pop();
			tracer.down("done with Class:"+fmx.getName());
		}
	}

	private void visit(IMethodDeclaration elt) {
		Method fmx = dico.createMethod(elt.getElementName(), (Type)context.top());
		fmx.setIsStub(false);
		tracer.msg("created Method:"+fmx.getName());
	}

	private void visit(IMethod elt) {
	}

	private void visit(IFunctionDeclaration elt) {
	}

	private void visit(IFunction elt) {
	}

	private void visit(IField elt) {
		Attribute fmx = dico.createAttribute(elt.getElementName(), (Type)context.top());
		fmx.setIsStub(false);
		tracer.msg("created Method:"+fmx.getName());
	}

	private void visit(IVariable elt) {
	}

	private void visit(IVariableDeclaration elt) {
	}

	private void visit(ITypeDef elt) {
	}


	// UTILITIES ======================================================================================================

	private void visitChildren(IParent elt) {
		try {
			for (ICElement child : elt.getChildren()) {
				child.accept(this);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
