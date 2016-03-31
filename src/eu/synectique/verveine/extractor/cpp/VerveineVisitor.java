package eu.synectique.verveine.extractor.cpp;

import java.io.File;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;

import eu.synectique.verveine.core.gen.famix.Entity;

public class VerveineVisitor extends ASTVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected Map<IBinding,Entity> dico;

	public VerveineVisitor(Map<IBinding,Entity> dico) {
		super(/*visitNodes*/true);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;*/
	    
		this.dico = dico;
	}

	protected boolean isBound(IASTName name) {
		return name.getBinding() != null;
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
		traceIndent = traceIndent.substring(0, traceIndent.length()-2);
	}
	protected void tracedown(String msg) {
		tracedown();
		tracemsg("Leaving "+msg);
	}

	// more specialized trace functions

	protected void tracename(IASTName name) {
		if (name != null) {
			tracemsg("    -> '"+name.toString()+ "' " +(isBound(name)?"is":"not") +" bound");
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
