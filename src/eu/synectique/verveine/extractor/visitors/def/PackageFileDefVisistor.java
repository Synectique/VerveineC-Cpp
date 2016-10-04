package eu.synectique.verveine.extractor.visitors.def;

import java.io.File;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.Module;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractDispatcherVisitor;

public class PackageFileDefVisistor extends AbstractDispatcherVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	protected boolean isCModel;

	/**
	 * Leading directory are the path of the project.
	 * We do not create packages for these, so we must count how deep in the directory hierarchy we are,
	 * to know when to create packages
	 */
	protected int nbOfLeadingDirectory;

	/**
	 * Prefix to remove from file names
	 */
	protected String rootFolder;

	public PackageFileDefVisistor(CDictionary dico, IIndex index, String rootFolder, boolean isCModel) {
		super(dico, index);
		this.rootFolder = rootFolder;
		this.isCModel = isCModel;
		nbOfLeadingDirectory = 2;  // i.e. tempProj/tempProj
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
		if (isCModel) {
			Module fmx;
			String filename = elt.getFile().getFullPath().toString();
			IBinding key = StubBinding.getInstance(Module.class, filename);
			filename = FileUtil.basename(filename);

			fmx = dico.ensureFamixModule( key, filename, currentPackage);
			fmx.setIsStub(false);
		}
		
		// no need to visit AST
	}

}
