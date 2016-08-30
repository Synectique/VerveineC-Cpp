package eu.synectique.verveine.extractor.utils;

import eu.synectique.verveine.extractor.visitors.AbstractVisitor;
import eu.synectique.verveine.extractor.visitors.ref.InvocationAccessRefVisitor;
import eu.synectique.verveine.extractor.visitors.ref.RefVisitor;

public class SubVisitorFactory {

	/*
	 * Could have used a generic method, but it would mean playing with the reflexive API which is not nice
	 */
	
	public static InvocationAccessRefVisitor createSubVisitorFCV( AbstractVisitor parentVisitor) {
		InvocationAccessRefVisitor visitor = new InvocationAccessRefVisitor(parentVisitor.getDico(), parentVisitor.getIndex());
		visitor.setContext( parentVisitor.getContext()); 
		return visitor;
	}

	public static RefVisitor createSubVisitorRV(InvocationAccessRefVisitor parentVisitor) {
		RefVisitor visitor = new RefVisitor(parentVisitor.getDico(), parentVisitor.getIndex());
		visitor.setContext( parentVisitor.getContext()); 
		return visitor;
	}

}
