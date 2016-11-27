package eu.synectique.verveine.extractor.visitors.def;

import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.extractor.plugin.CDictionary;

/**
 * A visitor for Behavioural entities: Functions and methods.
 * To deal with methods, it inherits from {@link ClassMemberDefVisitor}
 * 
 * There are 2 main entry pints in this visitor
 * <ul>
 * <li> visit(IASTStandardFunctionDeclarator)</li>
 * <li> visit(ICASTKnRFunctionDeclarator)</li>
 * </ul>

 * For reference, the inheritance hierarchy of function declarator nodes:
 * <ul>
 * <li> IASTFunctionDeclarator
 *   <ul>
 *   <li> IASTStandardFunctionDeclarator
 *     <ul>
 *     <li> ICPPASTFunctionDeclarator
 *       <ul>
 *       <li> ICPPASTFunctionTryBlockDeclarator</li>
 *       </ul>
 *     </li>
 *     </ul>
 *   </li>
 *   <li>ICASTKnRFunctionDeclarator</li>
 *   </ul>
 * </li>
 * </ul>
 * 
 * @author anquetil
 */
public class BehaviouralDefVisitor extends ClassMemberDefVisitor {
	
	/**
	 * flag describing whether we are visiting K&R function style parameters.
	 * Needed because these parameters look like variable (or attribute) definition)
	 */
	private boolean inKnRParams;

	public BehaviouralDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
		inKnRParams = false;
	}

	protected String msgTrace() {
		return "creating methods and functions";
	}

	//  MAIN ENTRY POINTS --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTStandardFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		// get node name and bnd
		super.visit( node);
		if (nodeBnd instanceof IVariable) {
			// declaration of a function pointer such as var in "int (*var)(int param1, char param2)"
			fmx = null;
		}
		else {
			fmx = initializeBehavioural(node);
			visitParameters(node.getParameters(), fmx);
		}
		returnedEntity = fmx;

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICASTKnRFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		// get node name and bnd
		super.visit( node);
		fmx = initializeBehavioural(node);

		inKnRParams = true;
		visitParameters( node.getParameterDeclarations(), fmx);
		inKnRParams = false;

		returnedEntity = fmx;

		return PROCESS_SKIP;
	}

	// OTHER VISIT METHOD --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;

		node.getDeclarator().accept(this);
		fmx = (BehaviouralEntity) returnedEntity;

		// FIXME using pushBehaviouralEntity()/pushMethod() introduces a difference in the handling of the stack (for metrics CYCLO/NOS)
		// this behaviour was inherited from VerveineJ and would need to be refactored
		this.getContext().pushBehaviouralEntity(fmx);

		// now visiting the children of the node
		node.getDeclSpecifier().accept(this);

		getContext().setLastAccess(null);		// TODO remove these 3 lines
		getContext().setLastInvocation(null);
		getContext().setLastReference(null);
		getContext().setTopMethodCyclo(1);
		getContext().setTopMethodNOS(0);

		node.getBody().accept(this);

		fmx.setNumberOfStatements(getContext().getTopMethodNOS());
		fmx.setCyclomaticComplexity(getContext().getTopMethodCyclo());
		this.getContext().pop();

		return PROCESS_SKIP;  // we already visited the children
	}

	/*
	 * We treat parameters in this visitor. could have used a separate visitor ...
	 * 
	 * Could use parameter node of type: ICPPASTParameterDeclaration as we re-dispatch to this in the  AbstractDispatcherVisitor
	 * But this way we get C and C++ parameters
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		 // get node name and bnd, may fail (e.g. "function(void)")
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}

		innerCreateParameter();

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		node.getDeclaration().accept(this);
		// redefined to not visit template parameters (see visit(IASTParameterDeclaration node))
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTSimpleDeclaration node) {
		if (declarationIsTypedef(node)) {
			// prune typedefs
			return PROCESS_SKIP;
		}

		// K&R style parameter definition
		if (inKnRParams) {
			for (IASTDeclarator decl : node.getDeclarators()) {
				decl.accept(this);
			}
		}

		return PROCESS_CONTINUE;
	}

	/**
	 * dealing with K&R style parameter declaration
	 */
	@Override
	protected int visitInternal(IASTDeclarator node) {
		if (inKnRParams) {
			nodeName = node.getName();
			nodeBnd = resolver.getBinding(nodeName);
			if (nodeBnd == null) {
				nodeBnd = resolver.mkStubKey(nodeName, Parameter.class);
			}

			innerCreateParameter();
		}

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
	    	getContext().addTopMethodCyclo(1);
	    }
	    
	    if ( (node instanceof IASTCaseStatement)	||    // not a statement but a case clause in a switch
		     (node instanceof IASTCompoundStatement)||    // block
		     (node instanceof IASTDefaultStatement)	||    // not a statement but the default clause in a switch
		     (node instanceof IASTLabelStatement) ) {
	    	// nothing to do for these, all the rest counts as one statement (in the else clause)
	    }
	    else {
	    	getContext().addTopMethodNOS(1);
	    }

		return super.visit(node);
	}


	// common parts to visiting IASTStandardFunctionDeclarator and ICASTKnRFunctionDeclarator


	protected BehaviouralEntity initializeBehavioural(IASTFunctionDeclarator node) {
		BehaviouralEntity fmx;

		fmx = resolver.ensureBehavioural(node, nodeBnd, nodeName);
		dico.setVisibility(fmx, currentVisibility);

		// parent node is a SimpleDeclaration or a FunctionDefinition
		IASTFileLocation defLoc = node.getParent().getFileLocation();
		dico.addSourceAnchorMulti(fmx, filename, defLoc);

		if (resolver.isDestructorBinding(nodeBnd)) {
			((Method)fmx).setKind(CDictionary.DESTRUCTOR_KIND_MARKER);
		}
		if (resolver.isConstructorBinding(nodeBnd)) {
			((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
		}
		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 

		return fmx;
	}

	@Override
	protected void visitParameters(IASTNode[] params, BehaviouralEntity fmx) {

		fmx.setNumberOfParameters(params.length);
		// note that there are 2 ways to get the number of parameters of a BehaviouralEntity in Famix: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here),
		// the second computes the size of parameter list so does not need to be set per se

		super.visitParameters(params, fmx);
	}

	protected Parameter innerCreateParameter() {
		Parameter fmx;
		fmx = dico.ensureFamixParameter(nodeBnd, nodeName.toString(), getContext().topBehaviouralEntity());
		fmx.setIsStub(false);
		// no sourceAnchor for parameter, they sometimes only appear in the .C file
		// whereas it would seem more natural to store the anchor referent to the .H file ...
		return fmx;
	}

}
