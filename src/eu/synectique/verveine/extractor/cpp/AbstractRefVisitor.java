package eu.synectique.verveine.extractor.cpp;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;

import eu.synectique.verveine.extractor.utils.Tracer;

/**
 * AST Visitor that defines all the (Famix) entities of interest
 * Famix entities are stored in a Map along with the IBindings to which they correspond
 */
public abstract class AbstractRefVisitor extends ASTVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionary dico;

	protected Tracer tracer = new Tracer();

	public AbstractRefVisitor(CDictionary dico) {
		super(/*visitNodes*/true);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;*/
	    this.dico = dico;
	}

}
