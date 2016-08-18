package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.extractor.plugin.CDictionary;

/**
 * A visitor for Behavioural entities: Functions and methods.
 * For methods, it inherits from {@link ClassMemberDefVisitor}
 * @author anquetil
 */
public class BehaviouralDefVisitor extends ClassMemberDefVisitor {

	public BehaviouralDefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	
}
