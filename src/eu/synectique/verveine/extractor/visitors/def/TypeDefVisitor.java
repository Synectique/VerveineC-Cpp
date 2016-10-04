package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
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
	public TypeDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	protected String msgTrace() {
		return "creating classes and types";
	}

	/**
	 * get Package associated to file directory
	 */
	@Override
	public void visit(ICContainer elt) {
		IBinding key = StubBinding.getInstance(Package.class, dico.mooseName(currentPackage, elt.getElementName()));

		currentPackage = (Package) dico.getEntityByKey(key);

		super.visit(elt);                                // visit children

		if (currentPackage != null) {
			currentPackage = currentPackage.getParentPackage();    // back to parent package
		}
	}

	@Override
	protected int visit(IASTSimpleDeclaration node) {
		TypeAlias aliasType = null;
		Type concreteType = null;

		if (declarationIsTypedef(node)) {
			boolean functionPointerTypedef = false;

			if (typedefToFunctionPointer(node)) {
				concreteType = null;  // TODO create a FunctionPointer special type?
				functionPointerTypedef = true;
			}
			else {
				returnedEntity = null;
				node.getDeclSpecifier().accept(this);
				concreteType = (Type) returnedEntity;
			}

			for (IASTDeclarator declarator : node.getDeclarators()) {
			// this is a typedef, so the declarator(s) should be FAMIXType(s)

				if (functionPointerTypedef) {
					nodeName = declarator.getNestedDeclarator().getName();
				}
				else {
					nodeName = declarator.getName();
				}

				nodeBnd = getBinding(nodeName);

				if (nodeBnd == null) {
					// create one anyway, assume this is a Type
					nodeBnd = StubBinding.getInstance(Type.class, dico.mooseName(context.getTopCppNamespace(), nodeName.toString()));
				}

				declarator.accept(this);

				aliasType = dico.ensureFamixTypeAlias(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());
				aliasType.setIsStub(false);
				aliasType.setParentPackage(currentPackage);
				aliasType.setAliasedType(concreteType);
			}
			
			return PROCESS_SKIP;  // typedef already handled
		}
		return PROCESS_CONTINUE;
	}

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;
		boolean isTemplate = definitionOfATemplate;
		definitionOfATemplate = false;   // Immediately turn it off because it could pollute visiting the children

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

	/** Visiting a struct in C
	 * similar to C++ but no template
	 */
	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		Class fmx;

		// compute nodeName and binding
		super.visit(node);
		fmx = dico.ensureFamixClass(nodeBnd, "struct "+nodeName.toString(), (ContainerEntity)context.top());

		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 
		dico.addSourceAnchor(fmx, filename, node.getFileLocation());

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		definitionOfATemplate = true;
		node.getDeclaration().accept(this);

		return PROCESS_SKIP;
	}

	/*
	 * Overriding just to turn off definitionOfATemplate in case this is a method template
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		definitionOfATemplate = false;

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
		eu.synectique.verveine.core.gen.famix.Enum fmx;

		nodeBnd = null;
		nodeName = node.getName();
		

		if (nodeName.equals("")) {
			// case of anonymous enum: it is probably within a typedef and will never be used directly
			// so the key is mostly irrelevant, only used to find back the type when creating its enumerated values 
			nodeBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Enum.class, dico.mooseName(context.topBehaviouralEntity(), ""+node.getFileLocation().getNodeOffset()));
		}
		else {
			nodeBnd = getBinding(nodeName);
			if (nodeBnd == null) {
				nodeBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Enum.class, dico.mooseName(context.topBehaviouralEntity(), nodeName.toString()));
			}
		}
		fmx = dico.ensureFamixEnum(nodeBnd, nodeName.toString(), (ContainerEntity)context.top(), /*persistIt*/true);
		dico.addSourceAnchor(fmx, filename, node.getFileLocation());

		returnedEntity = fmx;

		return PROCESS_SKIP;
	}

}
