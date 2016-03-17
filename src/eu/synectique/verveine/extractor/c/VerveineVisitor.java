package eu.synectique.verveine.extractor.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;

/**
 * AST Visitor that defines all the (Famix) entities of interest
 * Famix entities are stored in a Map along with the IBindings to which they correspond
 */
public class VerveineVisitor extends ASTVisitor {
	private String traceIndent="#";
	protected void traceup() {
		traceIndent += "  ";
	}
	protected void traceup(String msg) {
		tracemsg("Visiting "+msg);
		traceup();
	}
	protected void tracemsg(String msg) {
		System.err.println(traceIndent+"TRACE, "+msg);
	}
	protected void tracedown() {
		traceIndent = traceIndent.substring(0, traceIndent.length()-2);
	}
	protected void tracedown(String msg) {
		tracedown();
		tracemsg("Leaving "+msg);
	}

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CppDictionary dico;

	public VerveineVisitor(CppDictionary dico) {
		super(/*visitNodes*/true);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;*/
	    
		this.dico = dico;
	}

}