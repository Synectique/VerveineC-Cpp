package fr.verveine.visitors.def;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
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
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IVariable;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;
import fr.verveine.plugin.CDictionary;
import fr.verveine.utils.FileUtil;
import fr.verveine.utils.Trace;
import fr.verveine.utils.WrongClassGuessException;
import fr.verveine.visitors.AbstractVisitor;

/**
 * A visitor for Behavioural entities: Functions and methods.
 * To deal with methods, it inherits from {@link ClassMemberDefVisitor}
 * 
 * There are 2 main entry points in this visitor
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
	
	/**
	 * Whether to visit only header files (h/hpp) or only c/cpp files
	 */
	private boolean headerFiles;
	
	/**
	 * Whether we are in a function definition or declaration.
	 * This influences whether to create "real" {@link Parameter}s or "potential ones,
	 * see also {@link BehaviouralDefVisitor#privateCreateParameter()}. 
	 */
	private boolean isBehaviouralDefinition;

	/**
	 * A map that knows which parameters a "real" (or definitive) and which are "potential"
	 * The key is the parameter IBinding
	 */
	private Map<BehaviouralEntity,Boolean[]> mapBehavioural_IsdefinitiveParameters;

	/**
	 * A map that knows all the parameters of a behavioural in their correct order
	 * (BehaviouralEntity.getParameters() is based on a HashSet whichdoes not retain insertion order)
	 */
	private Map<BehaviouralEntity,Parameter[]> mapBehavioural_OrderedParameters;

	private Boolean[] isDefinitiveParameters;

	private Parameter[] orderedParameters;

 	public BehaviouralDefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
		inKnRParams = false;
		headerFiles = true;
		isBehaviouralDefinition = false;
		mapBehavioural_IsdefinitiveParameters = new HashMap<BehaviouralEntity, Boolean[]>();
		mapBehavioural_OrderedParameters = new HashMap<BehaviouralEntity, Parameter[]>();
	}

	protected String msgTrace() {
		return "creating methods and functions";
	}

	public void setHeaderFiles(boolean head) {
		this.headerFiles = head;
	}

	/**
	 * Overridden to visit only .h or .c files
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		// XOR operator: (headerFiles AND isHeader()) OR (!headerFiles AND !isHeader())
		if (! (headerFiles ^ FileUtil.isHeader(elt)) ) {
			super.visit(elt);
		}
	}

	//  MAIN ENTRY POINTS --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTStandardFunctionDeclarator node) {

		// get node name and bnd
		super.visit( node); // node.getParameters()
		if (nodeBnd == null) {
			// could not find the function (may happening if it is a actually a macro
			returnedEntity = null;
		}
		else if (nodeBnd instanceof IVariable) {
			// declaration of a function pointer such as var in "int (*var)(int param1, char param2)"
			returnedEntity = null;
		}
		else {
			returnedEntity = visitFunctionDeclarator(node, node.getParameters());
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICASTKnRFunctionDeclarator node) {
		// get node name and bnd
		super.visit( node); // node.getParameterDeclarations()

		inKnRParams = true;
		returnedEntity = visitFunctionDeclarator(node, node.getParameterDeclarations());
		inKnRParams = false;

		return PROCESS_SKIP;
	}

	// OTHER VISIT METHOD --------------------------------------------------------------------------------------------

	@Override
	protected int visit(IASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;

		isBehaviouralDefinition = true;
		node.getDeclarator().accept(this);
		fmx = (BehaviouralEntity) returnedEntity;
		isBehaviouralDefinition = false;

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
		
		if (node.getBody() != null) {
			// for method: Constructor() = delete;
			node.getBody().accept(this);
		}

		fmx.setNumberOfStatements(getContext().getTopMethodNOS());
		fmx.setCyclomaticComplexity(getContext().getTopMethodCyclo());
		this.getContext().pop();

		return PROCESS_SKIP;  // we already visited the children
	}

	/*
	 * We treat parameters in this visitor. could have used a separate visitor ...
	 * 
	 * Could use parameter node of type ICPPASTParameterDeclaration instead of IASTParameterDeclaration as we re-dispatch to this in the  AbstractDispatcherVisitor
	 * But this way we get C and C++ parameters
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		 // get node name and bnd, may fail (e.g. "function(void)")
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}

		privateCreateParameter();

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
			// prune typedefs because they define pointers to functions, not functions
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

			privateCreateParameter();
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


	protected BehaviouralEntity visitFunctionDeclarator(IASTFunctionDeclarator node, IASTNode[] params) {
		BehaviouralEntity fmx;
		String tmpFilename;
		Type classCtxt = null;

		if (declarationIsFriend(node)) {
			// friend function are outside the scope of the class
			// so remove the class from the context stack
			classCtxt = (Type) getContext().pop();
		}

		fmx = resolver.ensureBehavioural(node, nodeBnd, nodeName);
		dico.setVisibility(fmx, currentVisibility);

		// parent node is a SimpleDeclaration or a FunctionDefinition
		IASTFileLocation defLoc = node.getParent().getFileLocation();
		/* We can be in case where the function declaration being processed belongs to a file imported using an #includes statement
		 In such a case, filename instance variable will be initialized with location of the file "including" the external file. 
		 We should not use filename to generate the source anchor entity, as this is not reliable.
		 Instead, we should recompute a file location based on the current processed ast node.*/
		tmpFilename = FileUtil.localized(defLoc.getFileName(), rootFolder);
		dico.addSourceAnchorMulti(fmx, tmpFilename, defLoc);

		try {
			if (resolver.isDestructorBinding(nodeBnd)) {
				((Method)fmx).setKind(CDictionary.DESTRUCTOR_KIND_MARKER);
			}
			if (resolver.isConstructorBinding(nodeBnd)) {
				((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
			}
		}
		catch (ClassCastException e) {
			WrongClassGuessException.reportWrongClassGuess(Method.class, fmx);
		}

		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 

		visitParameters(params, fmx);

		if (declarationIsFriend(node)) {
			// friend function are outside the scope of the class
			// push back the class in the context stack
			getContext().push(classCtxt);
		}

		return fmx;
	}

	@Override
	protected void visitParameters(IASTNode[] params, BehaviouralEntity fmx) {
		// if there is only one parameter and it is "void" then there is no parameter
		if ( (params.length==1) && isVoidParam(params[0]) ) {
			fmx.setNumberOfParameters(0);
			return;
		}

		// note that there are 2 ways to get the number of parameters of a BehaviouralEntity in Famix: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here),
		// the second computes the size of parameter list so does not need to (and cannot) be set per se
		fmx.setNumberOfParameters(params.length);

		getParameterMaps(params.length, fmx); // there are 2 maps, so we are using global variables to store them instead of returning them
		super.visitParameters(params, fmx);   // ... modifies the parameterMaps ...
		setParameterMaps(fmx);                // ... and reset them
	}

	private boolean isVoidParam(IASTNode node) {
		IASTDeclSpecifier spec = null;

		if (node instanceof IASTParameterDeclaration) {
			spec = ((IASTParameterDeclaration)node).getDeclSpecifier();
		}
		else {
			if (node instanceof IASTSimpleDeclaration) {
				spec = ((IASTSimpleDeclaration)node).getDeclSpecifier();
			}
		}

		if (spec instanceof IASTSimpleDeclSpecifier) {
			return CDictionary.primitiveTypeName( ((IASTSimpleDeclSpecifier) spec).getType() ) == null ;
		}
		return false;
	}

	protected void setParameterMaps(BehaviouralEntity fmx) {
		mapBehavioural_IsdefinitiveParameters.put(fmx, isDefinitiveParameters);
		mapBehavioural_OrderedParameters.put(fmx, orderedParameters);
	}

	/**
	 * Create a parameter for a behavioural
	 * Relies on the facts that:
	 * <ul>
	 * <li> {@link AbstractVisitor#iParam} is the indice of this parameter in the parent behavioural list of parameters
	 * <li> {@link #isDefinitiveParameters} is a List of booleans for parent behavioural indicating whether
	 * its parameters are definitive or not. Said Booleans can also be null if the parameters were not created yet
	 * <li> {@link #behaviouralParameters} is an array of the Behavioural's Parameters (if they were already created)
	 * </ul>
	 * Creates the parameter and sets/modifies {@link #isDefinitiveParameters} as needed.
	 * Three situations of interest:
	 * <ul>
	 * <li>Parameter did not exist before and is created;
	 * <li>Parameter existed as a definitive one and it remains untouched;
	 * <li>Parameter existed as a "potential" one (from a Function declaration) and is now a definitive one (from a function definition),
	 * it must be potentially redefined
	 * </ul>
	 */
	protected Parameter privateCreateParameter() {
		Parameter fmx;
		
		if (! paramAlreadyExist(iParam)) {
			fmx = dico.ensureFamixParameter(nodeBnd, nodeName.toString(), getContext().topBehaviouralEntity());
			fmx.setIsStub(! isBehaviouralDefinition);
			isDefinitiveParameters[iParam] = isBehaviouralDefinition;
			orderedParameters[iParam] = fmx;
		}
		else {
			Parameter potential = getParam(iParam);
			if ( isBehaviouralDefinition && (! isDefinitiveParameters[iParam]) ) {
				if ( ! potential.getName().equals(nodeName.toString()) ) {
					// we are creating a "definitive" parameter, previous one existed as a "potential" parameter
					// but with a different name, so we need to change the parameter
					dico.removeParameter( potential);
					fmx = dico.ensureFamixParameter(nodeBnd, nodeName.toString(), getContext().topBehaviouralEntity());
				}
				else {
					fmx = potential;
				}
				if (fmx.getIsStub() && isBehaviouralDefinition) {
					fmx.setIsStub(false);
				}

				isDefinitiveParameters[iParam] = isBehaviouralDefinition;
				orderedParameters[iParam] = fmx;
			}
			else {
				fmx = getParam(iParam);
			}
		}
		
		// no sourceAnchor for parameter, they sometimes only appear in the .C file
		// whereas it would seem more natural to store the anchor referent to the .H file ...
		return fmx;
	}

	
	// UTILITIES ======================================================================================================

	protected void getParameterMaps(int nbParams, BehaviouralEntity fmx) {	
		orderedParameters = mapBehavioural_OrderedParameters.get(fmx);
		if (orderedParameters == null) {
			orderedParameters = new Parameter[nbParams];
		}

		isDefinitiveParameters = mapBehavioural_IsdefinitiveParameters.get(fmx);
		if (isDefinitiveParameters == null) {
			isDefinitiveParameters = new Boolean[nbParams];
		}
	}

	private boolean paramAlreadyExist(int iParam) {
		try {
			return (isDefinitiveParameters != null) && (isDefinitiveParameters[iParam] != null);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			return false;
		}
	}

	protected Parameter getParam(int iParam) {
		if (orderedParameters != null) {
			return orderedParameters[iParam];
		}
		else {
			return null;
		}
	}

	protected boolean declarationIsFriend(IASTFunctionDeclarator node) {
		IASTNode parentDecl = node.getParent();
		if (parentDecl instanceof IASTSimpleDeclaration) {
			IASTDeclSpecifier spec = ((IASTSimpleDeclaration)parentDecl).getDeclSpecifier();
			if (spec instanceof ICPPASTDeclSpecifier) {
				return ((ICPPASTDeclSpecifier) spec).isFriend();
			}
		}

		return false;
	}

}
