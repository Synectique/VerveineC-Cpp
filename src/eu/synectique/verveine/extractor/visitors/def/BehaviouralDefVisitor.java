package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Parameter;
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


	/**
	 * Visiting a method or function declaration (i.e. "header" aka signature)
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		// compute nodeName and binding
		super.visit(node);

		// just in case this is a definition and we already processed the declaration
		fmx = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);

		if (fmx == null) {
			fmx = resolveBehaviouralFromName(node, nodeBnd);
		}

		if (fmx != null) {
			// parent node is a SimpleDeclaration or a FunctionDefinition
			IASTFileLocation defLoc = node.getParent().getFileLocation();
			dico.addSourceAnchorMulti(fmx, defLoc.getFileName(), defLoc);
		}

		if (isDestructorBinding(nodeBnd)) {
			((Method)fmx).setKind(CDictionary.DESTRUCTOR_KIND_MARKER);
		}
		if (isConstructorBinding(nodeBnd)) {
			((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
		}
		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 
		fmx.setNumberOfParameters(node.getParameters().length);
		// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters

		this.context.push(fmx);

		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		returnedEntity = this.context.pop();

		return PROCESS_SKIP;
	}

	/**
	 * Visiting a method or function definition
	 */
	protected int visit(ICPPASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;

		this.visit( (ICPPASTFunctionDeclarator)node.getDeclarator());
		fmx = (BehaviouralEntity) returnedEntity;

		// using pushBehaviouralEntity()/pushMethod() introduces a difference in the handling of the stack (for metrics CYCLO/NOS)
		// this behaviour was inherited from VerveineJ and would need to be refactored
		this.context.pushBehaviouralEntity(fmx);

		// now visiting the children of the node
		node.getDeclSpecifier().accept(this);

		context.setLastAccess(null);
		context.setLastInvocation(null);
		context.setLastReference(null);
		context.setTopMethodCyclo(1);
		context.setTopMethodNOS(0);
		node.getBody().accept(this);

		if (fmx != null) {
			// don't remember exactly why it has to be that way
			// this was inherited from VerveineJ and requires some refactoring ...
			int nos = context.getTopMethodNOS();
			int cyclo = context.getTopMethodCyclo();
			fmx.setNumberOfStatements(nos);
			fmx.setCyclomaticComplexity(cyclo);
		}

		this.context.pop();

		return PROCESS_SKIP;  // we already visited the children
	}

	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		NamedEntity fmx = null;

		node.getDeclaration().accept(this);
		fmx = (NamedEntity) returnedEntity;

		// template parameters are local to the entity defined in the template declaration
		context.push(fmx);
        for (ICPPASTTemplateParameter param : node.getTemplateParameters()) {
        	if (param instanceof ICPPASTParameterDeclaration ) {
        		// i.e. a variable parameter to the template
        		// not sure exactly what it is, ignore for now
        	}
        	else if (param instanceof ICPPASTSimpleTypeTemplateParameter ) {
        		// a variable for a parameter type
 				dico.createParameterType( ((ICPPASTSimpleTypeTemplateParameter)param).getName().toString(), (ContainerEntity)context.top(), context.getTopCppNamespace());
        	}
        	else if (param instanceof ICPPASTTemplatedTypeTemplateParameter ) {
        		// a variable for a parameter type that is itself a template
        		dico.createParameterType( ((ICPPASTSimpleTypeTemplateParameter)param).getName().toString(), (ContainerEntity)context.top(), context.getTopCppNamespace());
        	}
        	// else should not happen
        }
        context.pop();

		return PROCESS_SKIP;
	}

	

	/*
	 * We treat parameters in this visitor. could have used a specialized sub-visitor ...
	 * 
	 * Could use parameter node of type: ICPPASTParameterDeclaration as we re-dispatch to this in the  AbstractDispatcherVisitor
	 * But this way we get C and C++ parameters
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		Parameter fmx = null;

		 // get node name and bnd
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}

		fmx = dico.ensureFamixParameter(nodeBnd, nodeName.toString(), context.topBehaviouralEntity());
		fmx.setIsStub(false);
		// no sourceAnchor for parameter, they sometimes only appear in the .C file
		// whereas it would seem more natural to store the anchor referent to the .H file ...

		return PROCESS_SKIP;
	}



	/**
	 * Visiting a statement. Used to compute some metrics on the methods:
	 * Cyclomatic Complexity and NumberOfStatements
	 */
	@Override
	public int visit(IASTStatement node) {
		// possible subtypes:
		// IASTBreakStatement, IASTCaseStatement, IASTCompoundStatement, IASTContinueStatement, IASTDeclarationStatement, IASTDefaultStatement,
		// IASTDoStatement, IASTExpressionStatement, IASTForStatement, IASTGotoStatement, IASTIfStatement, IASTLabelStatement, IASTNullStatement,
		// IASTProblemStatement, IASTReturnStatement, IASTSwitchStatement, IASTWhileStatement, ICPPASTCatchHandler, ICPPASTCompoundStatement,
		// ICPPASTForStatement, ICPPASTIfStatement, ICPPASTRangeBasedForStatement, ICPPASTSwitchStatement, ICPPASTTryBlockStatement, ICPPASTWhileStatement,
		// IGNUASTGotoStatement
	    if ( (node instanceof IASTCaseStatement)			||
	    	 (node instanceof IASTDefaultStatement)			||
	    	 (node instanceof IASTDoStatement)				||
	    	 (node instanceof IASTForStatement)				||
	    	 (node instanceof IASTIfStatement)				||
	    	 (node instanceof IASTWhileStatement)			||
	    	 (node instanceof ICPPASTRangeBasedForStatement)||
	    	 (node instanceof ICPPASTTryBlockStatement)		)  {
	    	context.addTopMethodCyclo(1);
	    }
	    
	    if ( (node instanceof IASTCaseStatement)	||
		     (node instanceof IASTCompoundStatement)||
		     (node instanceof IASTDefaultStatement)	||
		     (node instanceof IASTLabelStatement)	||
		     (node instanceof IASTNullStatement)	) {
	    	// nothing to do, it's all in the else clause
	    }
	    else {
	    	context.addTopMethodNOS(1);
	    }

		return super.visit(node);
	}

}
