package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractDispatcherVisitor;

public class PackageDefVisistor extends AbstractDispatcherVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	public PackageDefVisistor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	protected String msgTrace() {
		return "creating packages";
	}

	/**
	 * File directories are treated as Package
	 */
	@Override
	public void visit(ICContainer elt) {
		eu.synectique.verveine.core.gen.famix.Package fmx;
		fmx = dico.ensureFamixPackage(elt.getElementName(), currentPackage);
		fmx.setIsStub(false);

		currentPackage = fmx;
		super.visit(elt);
		currentPackage = fmx.getParentPackage();
	}

	public void visit(ITranslationUnit elt) {
		// prune AST visit on files
	}

}
