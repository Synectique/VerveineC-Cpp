package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;

import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class TypeDefVisitor extends AbstractVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected Package currentPackage = null;

	/**
	 * used between {@link #visit(ICPPASTTemplateDeclaration)} and {@link #visit(ICPPASTCompositeTypeSpecifier)}
	 * to mark class definitions that are FAMIXParameterizableClass
	 */
	protected boolean definitionOfATemplate = false;

	// CONSTRUCTOR ==========================================================================================================================

	/**
	 * Default constructor for definition pass
	 * @param dico where entities are created
	 * @param index CDT index containing bindings
	 */
	public TypeDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	/**
	 * get Package associated to file directory
	 */
	@Override
	public void visit(ICContainer elt) {
		IBinding key = StubBinding.getInstance(Package.class, dico.mooseName(currentPackage, elt.getElementName()));

		currentPackage = (Package) dico.getEntityByKey(key);

		super.visit(elt);                                // visit children

		currentPackage = currentPackage.getParentPackage();    // back to parent package
	}

	@Override
	protected int visit(IASTSimpleDeclaration node) {
		if (declarationIsTypedef(node)) {
			for (IASTDeclarator declarator : node.getDeclarators()) {
			// this is a typedef, so the declarator(s) should be FAMIXType(s)

				nodeName = declarator.getName();

				tracer.msg("IASTSimpleDeclaration (typedef):"+nodeName.toString());

				nodeBnd = getBinding(nodeName);

				if (nodeBnd == null) {
					// create one anyway, assume this is a Type
					nodeBnd = StubBinding.getInstance(Type.class, dico.mooseName(context.getTopCppNamespace(), nodeName.toString()));
				}

				/* Call back method.
				 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
				 * thus several nodeName and bnd
				 */
				visitSimpleTypeDeclaration(node);
			}
			
			return PROCESS_SKIP;  // typedef already handled
		}
		return PROCESS_CONTINUE;
	}

	/**
	 * Call back method from {@link #visit(IASTSimpleDeclaration)}.
	 * Creates an AliasType for the definedType.
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 * @param node not used in DefVisitor
	 */
	protected void visitSimpleTypeDeclaration(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = dico.ensureFamixTypeAlias(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());

		fmx.setIsStub(false);

		fmx.setParentPackage(currentPackage);
	}

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;
		boolean isTemplate = definitionOfATemplate;
		definitionOfATemplate = false;   // Immediately put it to false because it could pollute visiting the children

		// compute nodeName and binding
		super.visit(node);

		if (isTemplate) {
			fmx = dico.ensureFamixParameterizableClass(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		else {
			// if node is a stub with a fully qualified name, its parent is not context.top() :-(
			fmx = dico.ensureFamixClass(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 
		fmx.setParentPackage(currentPackage);
		
		// dealing with template class/struct
		if (isTemplate) {
			dico.addSourceAnchor(fmx, filename, ((ICPPASTTemplateDeclaration)node.getParent().getParent()).getFileLocation());
		}
		else {
			dico.addSourceAnchor(fmx, filename, node.getFileLocation());
		}

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}


	@Override
	public int visit(ICPPASTTemplateParameter node) {
		if (node instanceof ICPPASTSimpleTypeTemplateParameter) {
			nodeName = ((ICPPASTSimpleTypeTemplateParameter)node).getName();
			try {
				nodeBnd = index.findBinding(nodeName);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (nodeBnd != null) {
				returnedEntity = dico.ensureFamixParameterType(nodeBnd, nodeName.toString(), (ContainerEntity) context.top(), /*persistIt*/true);
			}
		}
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		definitionOfATemplate = true;
		returnedEntity = null;
		node.getDeclaration().accept(this);

		if (returnedEntity != null) {
			// we were visiting a template class (not a template method)

			// template parameters are local to the parameterizable class defined in the template declaration
			context.push((Type)returnedEntity);
			for (ICPPASTTemplateParameter param : node.getTemplateParameters()) {
				if (param instanceof ICPPASTSimpleTypeTemplateParameter ) {
					// a variable for a parameter type
					dico.createParameterType( ((ICPPASTSimpleTypeTemplateParameter)param).getName().toString(), (ContainerEntity)context.top(), context.getTopCppNamespace());
				}
				else if (param instanceof ICPPASTTemplatedTypeTemplateParameter ) {
					// a variable for a parameter type that is itself a template
					dico.createParameterType( ((ICPPASTSimpleTypeTemplateParameter)param).getName().toString(), (ContainerEntity)context.top(), context.getTopCppNamespace());
				}
				else {
					// ICPPASTParameterDeclaration not sure exactly what it means
					throw new IllegalStateException("Unrecognized type for ICPPASTTemplateParameter "+param.getRawSignature()+"in file "+node.getContainingFilename());
				}
			}
			context.pop();
		}

		return PROCESS_SKIP;
	}

	/*
	 * Overriding just to turn off definitionOfATemplate in case this is a method template
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		definitionOfATemplate = false;   // Immediately put it to false because it could pollute visiting the children
		returnedEntity = null;
		return PROCESS_SKIP;
	}


}
