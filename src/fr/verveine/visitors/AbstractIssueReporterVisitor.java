package fr.verveine.visitors;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.index.IIndex;

import fr.verveine.plugin.CDictionary;

/**
 * Abstract class for visitors that will collect issues on the AST and report them
 */
public abstract class AbstractIssueReporterVisitor extends AbstractDispatcherVisitor {

	/**
	 * A _set_ of all unresolved includes so that we report them only once
	 */
	protected Set<String> issues;

	public AbstractIssueReporterVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
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
