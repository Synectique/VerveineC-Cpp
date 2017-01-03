package eu.synectique.verveine.extractor.visitors;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.extractor.plugin.CDictionary;

public abstract class AbstractIssueReporterVisitor extends AbstractVisitor {

	/**
	 * A set of all unresolved includes so that we report them only once
	 */
	protected Set<String> issues;

	public AbstractIssueReporterVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
		issues = new HashSet<String>();
	}

	protected String msgTrace() {
		return "checking "+issueMsgTrace();
	}

	abstract protected String issueMsgTrace();

	public int nbIssues() {
		return issues.size();
	}

	public Iterable<String> getIssues() {
		return issues;
	}

	public void addIssues(String issue) {
		if (! issues.contains(issue)) {
			issues.add(issue);
		}
	}

	public void reportIssues() {
		this.reportIssues(System.out);
	}
	
	public void reportIssues(PrintStream st) {
		if (nbIssues() > 0) {
			st.println("There were "+nbIssues()+" "+issueMsgTrace());
			for (String str : issues) {
				st.println("  "+ str);
			}
		}
	}

}
