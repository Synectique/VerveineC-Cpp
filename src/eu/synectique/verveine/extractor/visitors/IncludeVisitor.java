package eu.synectique.verveine.extractor.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.CFile;
import eu.synectique.verveine.core.gen.famix.Module;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.plugin.VerveineCParser;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class IncludeVisitor extends AbstractDispatcherVisitor {

	protected CFile currentFile;

	/**
	 * Prefix to remove from file names
	 */
	protected String absoluteRootFolder;
	protected String projectRootFolder;

	/**
	 * A set of all unresolved includes so that we report them only once
	 */
	protected Set<String> unresolvedIncludes;

	public IncludeVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index);
		unresolvedIncludes = new HashSet<String>();
		this.absoluteRootFolder = rootFolder;
		int i = rootFolder.indexOf(VerveineCParser.WORKSPACE_NAME);
		if (i > 0) {		
			this.projectRootFolder = rootFolder.substring(i+VerveineCParser.WORKSPACE_NAME.length());
		}
	}

	protected String msgTrace() {
		return "checking unresolved includes";
	}

	@Override
	public void visit(ITranslationUnit elt) {
		String filename = elt.getFile().getFullPath().toString();        // fullPath relative to project directory
		IBinding key = StubBinding.getInstance(CFile.class, filename);   // better not to localize filename for the key
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
			key = StubBinding.getInstance(Module.class, includedName);
			includedName = FileUtil.localized(includedName, absoluteRootFolder);
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
			key = StubBinding.getInstance(Module.class, includedName);
		}

		included = dico.ensureFamixCFile(key, includedName);
		dico.addFamixInclude(currentFile, included);
	}

	public int nbUnresolvedIncludes() {
		return unresolvedIncludes.size();
	}

	public void reportUnresolvedIncludes() {
		if (nbUnresolvedIncludes() > 0) {
			System.err.println("There were "+nbUnresolvedIncludes()+" unresolved includes");
			for (String str : unresolvedIncludes) {
				System.err.println("  "+ str);
			}
		}
	}
}
