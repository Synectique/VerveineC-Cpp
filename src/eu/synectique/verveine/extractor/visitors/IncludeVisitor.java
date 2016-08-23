package eu.synectique.verveine.extractor.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.IInclude;

import eu.synectique.verveine.extractor.plugin.CDictionary;

public class IncludeVisitor extends AbstractDispatcherVisitor {

	/**
	 * A set of all unresolved includes so that we report them only once
	 */
	protected Set<String> unresolvedIncludes;

	public IncludeVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
		unresolvedIncludes = new HashSet<String>();
	}

	protected String msgTrace() {
		return "checking unresolved includes";
	}

	public void visit(IInclude elt) {
		if (! elt.isResolved()) {
			String includeStr;
			includeStr = elt.isLocal() ? "\"" : "<";
			includeStr += elt.getIncludeName();
			includeStr += elt.isLocal() ? "\"" : ">";
			if (! unresolvedIncludes.contains(includeStr)) {
				unresolvedIncludes.add(includeStr);
			}
		}
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
