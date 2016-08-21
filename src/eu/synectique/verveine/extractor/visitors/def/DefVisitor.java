package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class DefVisitor extends AbstractVisitor implements ICElementVisitor {

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
	public DefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

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

	/*
	 * redefining to check for header files (or not)
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		if (checkHeader(elt)) {
			super.visit(elt);
		}
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

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


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

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
		returnedEntity = fmx;

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		returnedEntity = context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	
	// UTILITIES ==============================================================================================================================

	/**
	 * Call back method from {@link AbstractVisitor#visit(IASTSimpleDeclaration)}.
	 * Creates an AliasType for the definedType.
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 * @param node not used in DefVisitor
	 */
	@Override
	protected void visitSimpleTypeDeclaration(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = dico.ensureFamixTypeAlias(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());

		fmx.setIsStub(false);

		fmx.setParentPackage(currentPackage);
	}

}
