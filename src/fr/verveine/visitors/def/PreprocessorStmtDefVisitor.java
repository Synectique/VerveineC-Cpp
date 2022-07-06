package fr.verveine.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfdefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIfndefStatement;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorMacroDefinition;
import org.eclipse.cdt.core.dom.ast.IASTPreprocessorStatement;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.gen.famix.PreprocessorIfdef;
import fr.verveine.plugin.CDictionary;
import fr.verveine.visitors.AbstractVisitor;

public class PreprocessorStmtDefVisitor extends AbstractVisitor {

	public PreprocessorStmtDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
	}

	@Override
	protected String msgTrace() {
		return "creating preprocessor statements";
	}

	@Override
	public int visit(IASTTranslationUnit node) {
		if (isProjectFile(node.getFilePath())) {
			for (IASTPreprocessorStatement pstmt : node.getAllPreprocessorStatements()) {
				if (pstmt instanceof IASTPreprocessorIfdefStatement) {
					this.visit( (IASTPreprocessorIfdefStatement)pstmt);
				}
				else if (pstmt instanceof IASTPreprocessorIfndefStatement) {
					this.visit( (IASTPreprocessorIfndefStatement)pstmt);
				}
				else if (pstmt instanceof IASTPreprocessorMacroDefinition) {
					// nothing for now
				}
			}
		}

		return PROCESS_SKIP;
	}

	protected void visit(IASTPreprocessorIfdefStatement node) {
		IASTName macro = node.getMacroReference();
		if (macro != null) {
			PreprocessorIfdef fmx = dico.createFamixPreprocIfdef(macro.toString());
			setIfdefSourceAnchor(fmx, node.getFileLocation());
		}
	}

	protected void visit(IASTPreprocessorIfndefStatement node) {
		IASTName macro = node.getMacroReference();
		if (macro != null) {
			PreprocessorIfdef fmx = dico.createFamixPreprocIfdef(macro.toString());
			fmx.setNegated(true);
			setIfdefSourceAnchor(fmx, node.getFileLocation());
		}
	}

	

	private boolean isProjectFile(String filePath) {
		return filePath.startsWith(rootFolder);
	}

	protected void setIfdefSourceAnchor(PreprocessorIfdef fmx, IASTFileLocation defLoc) {
		int startPos;
		int endPos;

		startPos = defLoc.getNodeOffset();
		endPos = startPos + fmx.getMacro().length();
		dico.addSourceAnchor(fmx, filename, startPos, endPos);
	}

}
