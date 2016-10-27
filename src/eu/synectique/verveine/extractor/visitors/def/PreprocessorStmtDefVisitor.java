package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractDispatcherVisitor;

public class PreprocessorStmtDefVisitor extends AbstractDispatcherVisitor {

	public PreprocessorStmtDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	protected String msgTrace() {
		return "Counting preprocessor statements";
	}

	@Override
	public int visit(IASTTranslationUnit node) {
		// Handle all comments in this file
		for (IASTPreprocessorStatement pstmt : node.getAllPreprocessorStatements()) {
			/*int startPos = 0;
			int endPos = 0;

			Comment fmx = dico.createFamixComment(cmt.toString());

			IASTFileLocation defLoc = cmt.getFileLocation();
			startPos = defLoc.getNodeOffset();
			endPos = startPos + cmt.toString().length();
			dico.addSourceAnchor(fmx, filename, startPos, endPos);*/
			System.err.println("PStmt: "+ pstmt.toString());
		}
		return PROCESS_SKIP;
	}

}
