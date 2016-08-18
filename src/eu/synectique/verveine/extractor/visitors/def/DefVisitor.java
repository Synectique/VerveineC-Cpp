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
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTSimpleTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplatedTypeTemplateParameter;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

public class DefVisitor extends AbstractVisitor implements ICElementVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected Package currentPackage = null;

	/**
	 * used between {@link #visit(ICPPASTTemplateDeclaration)} and {@link #visit(ICPPASTCompositeTypeSpecifier)}
	 * to mark class definitions that are FAMIXParameterizableClass
	 */
	protected boolean definitionOfATemplate = false;

	// CONSTRUCTOR ==========================================================================================================================

	/**
	 * Default constructor for definition pass
	 * @param dico where entities are created
	 * @param index CDT index containing bindings
	 */
	public DefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

	/**
	 * get Package associated to file directory
	 */
	@Override
	public void visit(ICContainer elt) {
		IBinding key = StubBinding.getInstance(Package.class, dico.mooseName(currentPackage, elt.getElementName()));

		currentPackage = (Package) dico.getEntityByKey(key);

		super.visit(elt);                                // visit children

		currentPackage = currentPackage.getParentPackage();    // back to parent package
	}

	/*
	 * redefining to check for header files (or not)
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		if (checkHeader(elt)) {
			super.visit(elt);
		}
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

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

	@Override
	public int visit(ICPPASTTemplateParameter node) {
		if (node instanceof ICPPASTSimpleTypeTemplateParameter) {
			nodeName = ((ICPPASTSimpleTypeTemplateParameter)node).getName();
			try {
				nodeBnd = index.findBinding(nodeName);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			if (nodeBnd != null) {
				returnedEntity = dico.ensureFamixParameterType(nodeBnd, nodeName.toString(), (ContainerEntity) context.top(), /*persistIt*/true);
			}
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


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;
		boolean isTemplate = definitionOfATemplate;
		definitionOfATemplate = false;   // Immediately put it to false because it could pollute visiting the children

		// compute nodeName and binding
		super.visit(node);

		if (isTemplate) {
			fmx = dico.ensureFamixParameterizableClass(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		else {
			// if node is a stub with a fully qualified name, its parent is not context.top() :-(
			fmx = dico.ensureFamixClass(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 
		fmx.setParentPackage(currentPackage);
		
		// dealing with template class/struct
		if (isTemplate) {
			dico.addSourceAnchor(fmx, filename, ((ICPPASTTemplateDeclaration)node.getParent().getParent()).getFileLocation());
		}
		else {
			dico.addSourceAnchor(fmx, filename, node.getFileLocation());
		}

		this.context.push(fmx);
		returnedEntity = fmx;

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		returnedEntity = context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	/**
	 * Visiting a method or function declaration (i.e. "header" aka signature)
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		if (definitionOfATemplate) {
			// functions/methods can also be templates, but we are not interested in that
			definitionOfATemplate = false;
		}
	
		// compute nodeName and binding
		super.visit(node);

		// just in case this is a definition and we already processed the declaration
		fmx = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);

		if (fmx == null) {
			fmx = recoverBehaviouralManually(node, nodeBnd);
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
		tracer.up("ICPPASTFunctionDefinition");

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
		tracer.down();

		return PROCESS_SKIP;  // we already visited the children
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return super.leave(node);
	}

	@Override
	protected int visit(ICPPASTTemplateDeclaration node) {
		NamedEntity fmx = null;

		definitionOfATemplate = true;
		node.getDeclaration().accept(this);
		fmx = (NamedEntity) returnedEntity;
		definitionOfATemplate = false;       // as a security, in case we forgot to turn it off in a child node

		// template parameters are local to the entity defined in the template declaration
		context.push(fmx);
        for (ICPPASTTemplateParameter param : node.getTemplateParameters()) {
        	if (param instanceof ICPPASTParameterDeclaration ) {
        		// i.e. a variable parameter to the template
        		// not sure exactly what it is, ignore for now
        	}
        	else if (param instanceof ICPPASTSimpleTypeTemplateParameter ) {
        		// a variable for a parameter type
 				createParameterTypeInCurrentContext( (ICPPASTSimpleTypeTemplateParameter)param);
        	}
        	else if (param instanceof ICPPASTTemplatedTypeTemplateParameter ) {
        		// a variable for a parameter type that is itself a template
         		createParameterTypeInCurrentContext( (ICPPASTSimpleTypeTemplateParameter) param);
        	}
        	// else should not happen
        }
        context.pop();

		return PROCESS_SKIP;
	}

	/** 
	 * Creating a "parameter type" depends on the context
	 * <ul>
	 * <li> If it is a ParameterizableClass (e.g. "<code>template &lt;class T&gt; class C</code> ...", we create a ParameterType
	 * <li> If it is a Method (e.g. "<code>template &lt;class T&gt; void fct(T)</code> ..."), we create a Type
	 * </ul>
	 */
	protected eu.synectique.verveine.core.gen.famix.Type createParameterTypeInCurrentContext(ICPPASTSimpleTypeTemplateParameter paramTyp) {
		ContainerEntity owner = (ContainerEntity) context.top();
		// apparently CDT gives a binding to the parameterType at its declaration ("template <class T> ...")
		// but not when used ("... mth(T)") so we ignore CDT binding and always use our custom build one
    	IBinding bnd; // = getBinding(paramTyp.getName());
    	//if (bnd == null) {
    		bnd = StubBinding.getInstance(Type.class, dico.mooseName(getTopCppNamespace(), paramTyp.getName().toString()));
    	//}
		if (owner instanceof ParameterizableClass) {
			return dico.ensureFamixParameterType(bnd, paramTyp.getName().toString(), owner);
		}
		else {
			return dico.ensureFamixType(bnd, paramTyp.getName().toString(), owner);
		}
	}

	/** 
	 * Exactly the same as {@link #createParameterTypeInCurrentContext(ICPPASTSimpleTypeTemplateParameter)}, put the parameter is not the same type !
	 */
	protected eu.synectique.verveine.core.gen.famix.Type createParameterTypeInCurrentContext(ICPPASTTemplatedTypeTemplateParameter paramTyp) {
		ContainerEntity owner = (ContainerEntity) context.top();
    	IBinding bnd = getBinding(paramTyp.getName());
    	if (bnd == null) {
    		bnd = StubBinding.getInstance(Type.class, dico.mooseName(getTopCppNamespace(), paramTyp.getName().toString()));
    	}
		if (owner instanceof ParameterizableClass) {
			return dico.ensureFamixParameterType(bnd, paramTyp.getName().toString(), owner);
		}
		else {
			return dico.ensureFamixType(bnd, paramTyp.getName().toString(), owner);
		}
	}
	
	
	// UTILITIES ==============================================================================================================================

	/**
	 * Call back method from {@link AbstractVisitor#visit(IASTSimpleDeclaration)}.
	 * Creates an AliasType for the definedType.
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 * @param node not used in DefVisitor
	 */
	@Override
	protected void visitSimpleTypeDeclaration(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = dico.ensureFamixTypeAlias(nodeBnd, nodeName.toString(), (ContainerEntity)context.top());

		fmx.setIsStub(false);

		fmx.setParentPackage(currentPackage);
	}

}
