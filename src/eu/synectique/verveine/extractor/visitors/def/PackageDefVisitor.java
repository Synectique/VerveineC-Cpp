package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractDispatcherVisitor;

public class PackageDefVisitor extends AbstractDispatcherVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	/**
	 * Leading directory are the path of the project.
	 * We do not create packages for these, so we must remember how deep in the directory hierarchy we are,
	 * to know when to create packages
	 */
	protected int nbOfLeadingDirectory;

	public PackageDefVisitor(CDictionary dico) {
		super(dico, null);
		nbOfLeadingDirectory = 1;  // i.e. tempProj/
	}

	protected String msgTrace() {
		return "creating packages";
	}

	/**
	 * File directories are treated as Package
	 */
	@Override
	public void visit(ICContainer elt) {
		Package fmx = null;
		
		nbOfLeadingDirectory--; // one directory less

		if (nbOfLeadingDirectory < 0) {
			fmx = dico.ensureFamixPackage(elt.getElementName(), currentPackage);
			fmx.setIsStub(false);
			currentPackage = fmx;
		}
		
		super.visit(elt);
		
		if (nbOfLeadingDirectory < 0) {
			currentPackage = fmx.getParentPackage();
		}

		nbOfLeadingDirectory++; // going up one directory
	}


	/**
	 * Files are treated as Modules
	 */
	@Override
	public void visit(ITranslationUnit elt) {

	}

}
