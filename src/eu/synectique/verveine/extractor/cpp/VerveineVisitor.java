package eu.synectique.verveine.extractor.cpp;

import java.io.File;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;

import ch.akuhn.fame.Repository;

/**
 * AST Visitor that defines all the (Famix) entities of interest
 * Famix entities are stored in a Map along with the IBindings to which they correspond
 */
public class VerveineVisitor extends ASTVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionary dico;

	protected Repository famixRepo;
	
	public VerveineVisitor(CDictionary dico) {
		super(/*visitNodes*/true);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;*/
	    this.dico = dico;
	}

	// TRACING
	
	private String traceIndent="#";

	protected void traceup() {
		traceIndent += "  ";
	}
	protected void traceup(String msg) {
		tracemsg("Visiting "+msg);
		traceup();
	}
	protected void tracemsg(String msg) {
		System.err.println(traceIndent+msg);
	}
	protected void tracedown() {
		// protect against too may tracedown()
		if (traceIndent.length() > 2) {
			traceIndent = traceIndent.substring(0, traceIndent.length()-2);
		}
	}
	protected void tracedown(String msg) {
		tracedown();
		tracemsg("Leaving "+msg);
	}

	// more specialized trace functions

	protected void tracename(IASTName name) {
		if (name != null) {
			tracemsg("    -> '"+name.toString()+ "' " +(name.resolveBinding()!=null ?"is":"not") +" bound");
		}
		else {
			tracemsg("    -> null name");
		}
	}
	
	protected void traceanchor(IASTFileLocation loc) {
		String filename = loc.getFileName();
		filename = filename.substring(filename.lastIndexOf(File.separatorChar)+1);
		tracemsg("    -> fileanchor=<"+filename+","+loc.getNodeOffset()+">");
	}
}
