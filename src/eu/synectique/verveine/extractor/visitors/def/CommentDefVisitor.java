package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Comment;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.AbstractDispatcherVisitor;

public class CommentDefVisitor extends AbstractDispatcherVisitor {

	public CommentDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}
	
	protected String msgTrace() {
		return "extracting comments";
	}

	/*
	 * Redefined because no need to visit the children, only the AST
	 */
	public void visit(ITranslationUnit elt) {
		try {
			elt.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT | ITranslationUnit.AST_SKIP_INDEXED_HEADERS).accept(this);
		} catch (CoreException e) {
			System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ elt.getElementName() );
		}
	}

	@Override
	public int visit(IASTTranslationUnit node) {
		// Handle all comments in this file
		for (IASTComment cmt : node.getComments()) {
			Comment fmx = dico.createFamixComment(cmt.toString());
			IASTFileLocation defLoc = node.getFileLocation();
			dico.addSourceAnchor(fmx, defLoc.getFileName(), defLoc);
		}
		return PROCESS_SKIP;
	}

}
