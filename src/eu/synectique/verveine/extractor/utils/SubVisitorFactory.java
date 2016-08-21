package eu.synectique.verveine.extractor.utils;

import eu.synectique.verveine.extractor.visitors.AbstractVisitor;
import eu.synectique.verveine.extractor.visitors.ref.FunctionCallVisitor;
import eu.synectique.verveine.extractor.visitors.ref.RefVisitor;

public class SubVisitorFactory {

	/*
	 * Could have used a generic method, but it would mean playing with the reflexive API which is not nice
	 */
	
	public static FunctionCallVisitor createSubVisitorFCV( AbstractVisitor parentVisitor) {
		FunctionCallVisitor visitor = new FunctionCallVisitor(parentVisitor.getDico(), parentVisitor.getIndex());
		visitor.setContext( parentVisitor.getContext()); 
		return visitor;
	}

	public static RefVisitor createSubVisitorRV(FunctionCallVisitor parentVisitor) {
		RefVisitor visitor = new RefVisitor(parentVisitor.getDico(), parentVisitor.getIndex());
		visitor.setContext( parentVisitor.getContext()); 
		return visitor;
	}

}
