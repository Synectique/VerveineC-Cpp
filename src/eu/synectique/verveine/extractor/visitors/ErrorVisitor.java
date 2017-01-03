package eu.synectique.verveine.extractor.visitors;

import java.io.PrintStream;

import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.extractor.plugin.CDictionary;

public class ErrorVisitor extends AbstractIssueReporterVisitor {

	public ErrorVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	@Override
	protected String issueMsgTrace() {
		return "parsing errors";
	}

	@Override
	public void reportIssues() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportIssues(PrintStream st) {
		// TODO Auto-generated method stub
		
	}

}
