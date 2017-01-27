package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.IASTProblem;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.IProblem;

import eu.synectique.verveine.extractor.plugin.CDictionary;

public class ErrorVisitor extends AbstractIssueReporterVisitor {

	public ErrorVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index);
	}

	@Override
	protected String issueMsgTrace() {
		return "parsing errors";
	}

	@Override
	public int visit(IASTProblem node) {
		if (node instanceof IProblem) {
			String issue = "";
			IProblem problem = node;
			
			if (problem.isError()) {
				issue = "Error:";
			}
			else if (problem.isWarning()) {
				issue = "Warning:";
			}
			addIssues(issue + problem.getMessageWithLocation());
		}
		return super.visit(node);
	}


	
}
