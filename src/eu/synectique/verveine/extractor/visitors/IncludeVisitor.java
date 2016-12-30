package eu.synectique.verveine.extractor.visitors;

import java.io.File;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.IStatus;

import eu.synectique.verveine.core.gen.famix.CFile;
import eu.synectique.verveine.extractor.plugin.Activator;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.plugin.VerveineCParser;
import eu.synectique.verveine.extractor.utils.FileUtil;

public class IncludeVisitor extends AbstractVisitor {

	protected CFile currentFile;

	/**
	 * Prefix to remove from file names
	 */
	protected String projectRootFolder;

	/**
	 * A set of all unresolved includes so that we report them only once
	 */
	protected Set<String> unresolvedIncludes;

	/**
	 * Full path of the project root
	 */
	private String projPath;

	public IncludeVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
		unresolvedIncludes = new HashSet<String>();
		int i = rootFolder.indexOf(VerveineCParser.WORKSPACE_NAME);
		if (i > 0) {		
			this.projectRootFolder = rootFolder.substring(i+VerveineCParser.WORKSPACE_NAME.length());
		}
	}

	protected String msgTrace() {
		return "checking unresolved includes";
	}

	public void visit(ICProject project) {
		projPath = project.getLocationURI().getRawPath();
		int i = projPath.lastIndexOf(File.separator);
		if (i > 0) {
			projPath=projPath.substring(0, i);
		}
		super.visit(project);
	}

	@Override
	public void visit(ITranslationUnit elt) {
		String filename = elt.getFile().getFullPath().toString();        // fullPath relative to project directory
		IBinding key = resolver.mkStubKey(projPath+filename, /*container*/null, CFile.class);   // better not to localize filename for the key
		currentFile = dico.ensureFamixCFile(key, FileUtil.localized(filename, projectRootFolder));
		
		// overriding superclass visit() to not visit AST but only the children
		visitChildren(elt);
	}

	public void visit(IInclude elt) {
		CFile included;
		String includedName;
		IBinding key;
	
		if (elt.isResolved()) {
			includedName = elt.getFullFileName();                       // fullpath relative to the entire file system
			key = resolver.mkStubKey(includedName, /*container*/null, CFile.class);
			includedName = FileUtil.localized(includedName, rootFolder);
		}
		else {
			String includeStr;
			includeStr = elt.isLocal() ? "\"" : "<";
			includeStr += elt.getIncludeName();
			includeStr += elt.isLocal() ? "\"" : ">";
			if (! unresolvedIncludes.contains(includeStr)) {
				unresolvedIncludes.add(includeStr);
			}
			includedName = elt.getIncludeName();
			key = resolver.mkStubKey(includedName, /*container*/null, CFile.class);
		}

		included = dico.ensureFamixCFile(key, includedName);
		dico.addFamixInclude(currentFile, included);
	}

	public int nbUnresolvedIncludes() {
		return unresolvedIncludes.size();
	}

	public void reportUnresolvedIncludes() {
		if (nbUnresolvedIncludes() > 0) {
			Activator.log(IStatus.WARNING, "There were "+nbUnresolvedIncludes()+" unresolved includes");
			for (String str : unresolvedIncludes) {
				Activator.log(IStatus.WARNING, "  "+ str);
			}
		}
	}

	public Iterable<String> getUnresolvedIncludes() {
		return unresolvedIncludes;
	}
}
