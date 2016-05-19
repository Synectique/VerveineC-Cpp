package eu.synectique.verveine.extractor.visitors.ref;

/**
 * Abstract super class for all Ref sub-visitor.
 * These are helper visitors created by the "main" RefVisitor to  deal with special cases
 */
public class AbstractRefSubVisitor extends AbstractRefVisitor {

	public AbstractRefSubVisitor(AbstractRefVisitor parentVisitor) {
		super(parentVisitor.getDico(), parentVisitor.getIndex());
		context = parentVisitor.getContext(); 
	}

}
