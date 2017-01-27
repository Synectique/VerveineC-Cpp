package eu.synectique.verveine.extractor.visitors;

import java.io.File;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.CFile;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.plugin.VerveineCParser;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.NameResolver;

public class IncludeVisitor extends AbstractIssueReporterVisitor {

	protected CFile currentFile;

	/**
	 * Prefix to remove from file names
	 */
	protected String rootFolder;

	/**
	 * An object responsible for resolving names.
	 * This implies keeping track of the current context stack as well as handling the CDT IIndex, finding bindings for names,
	 * or dealing with name (fully-qualified or not)
	 */
	protected NameResolver resolver;

	/**
	 * Full path of the project root
	 */
	private String projPath;

	public IncludeVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index);
		
		// similar to what is done in AbstractVisitor, but we don't need many things that it does
		this.rootFolder = rootFolder;
		this.resolver = new NameResolver(dico, index);
	}

	protected String issueMsgTrace() {
		return "unresolved includes";
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
		currentFile = dico.ensureFamixCFile(key, FileUtil.localized(filename,  File.separator + VerveineCParser.DEFAULT_PROJECT_NAME + File.separator + VerveineCParser.SOURCE_ROOT_DIR + File.separator) );
		
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
			String includeStr = "Error:Unresolved include: ";
			includeStr += elt.isLocal() ? "\"" : "<";
			includeStr += elt.getIncludeName();
			includeStr += elt.isLocal() ? "\"" : ">";
			addIssues(includeStr);

			includedName = elt.getIncludeName();
			key = resolver.mkStubKey(includedName, /*container*/null, CFile.class);
		}

		included = dico.ensureFamixCFile(key, includedName);
		dico.addFamixInclude(currentFile, included);
	}

}
