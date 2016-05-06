package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.visitors.ref.CDictionary;

public class DefVisitor extends AbstractVisitor implements ICElementVisitor {

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack2 context;

	/**
	 * The file directory being visited at any given time
	 */
	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	/**
	 * A flag to allow visiting separately header files (.h) and source files (.c)
	 * The idea is to visit first the header files
	 */
	protected boolean visitHeaders;

	// CONSTRUCTOR ==========================================================================================================================

	/**
	 * Default constructor for definition pass
	 * @param dico where entities are created
	 * @param index CDT index containing bindings
	 */
	public DefVisitor(CDictionary dico, IIndex index) {
		super(dico, index, /*visitNodes*/true);

		tracer = new NullTracer("DEF>");
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

	/**
	 * Visit an file directory. They are treated as Package
	 */
	@Override
	public void visit(ICContainer elt) {
		tracer.up("ICContainer: "+elt.getElementName());
		eu.synectique.verveine.core.gen.famix.Package fmx;
		fmx = dico.ensureFamixPackage(elt.getElementName(), currentPackage);
		fmx.setIsStub(false);

		currentPackage = fmx;                        // kind of pushing new package on a virtual package stack
		super.visit(elt);                            // visit container children
		currentPackage = fmx.getParentPackage();    // kind of popping out the new package from the package stack
		tracer.down();
	}

	/**
	 * Visiting a source file
	 */
	@Override
	public void visit(ITranslationUnit tu) {
		if (checkHeader(tu)) {
			tracer.up("ITranslationUnit: "+tu.getElementName());
			context = new EntityStack2();    // "reseting" context
			super.visit(tu);
			tracer.down();
		}
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		Namespace fmx;
		IASTName nodeName = node.getName();
		IIndexBinding bnd = null;

		nodeName = node.getName();

		try {
			bnd = index.findBinding(nodeName);
		}
		catch (CoreException e) {
			e.printStackTrace();
		}
		fmx = dico.ensureFamixNamespace(bnd, nodeName.getLastName().toString(), (Namespace) this.context.top());

		if (fmx != null) {
			this.context.push(fmx);
		}

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclaration node) {
		if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		return super.leave(node);
	}

	/**
	 * Visiting a function Parameter. Only creates the FamixParameter
	 */
	@Override
	public int visit(IASTParameterDeclaration node) {
		IASTDeclarator nodeParam = node.getDeclarator();
		IASTName nodeName = nodeParam.getName();
		IIndexBinding bnd = null;
		Parameter fmx;

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		fmx = dico.ensureFamixParameter(bnd, nodeName.toString(), context.topMethod());

		fmx.setIsStub(false);

		dico.addSourceAnchor(fmx, filename, node.getFileLocation());

		return ASTVisitor.PROCESS_SKIP;
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
		IASTName nodeName;
		IIndexBinding bnd = null;
		Class fmx;
		boolean isTemplate;

		nodeName = node.getName();

		isTemplate = (node.getParent().getParent() instanceof ICPPASTTemplateDeclaration);

		if (nodeName == null) {
			return PROCESS_SKIP;
		}

		tracer.up("ICPPASTCompositeTypeSpecifier:"+nodeName.toString());

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		if (isTemplate) {
			fmx = dico.ensureFamixParameterizableClass(bnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		else {
			fmx = dico.ensureFamixClass(bnd, nodeName.toString(), (ContainerEntity)context.top());
		}

		if (fmx == null) {
			return PROCESS_SKIP;
		}

		fmx.setIsStub(false);

		fmx.setParentPackage(currentPackage);
		
		// dealing with template class/struct
		if (isTemplate) {
			dico.addSourceAnchor(fmx, filename, ((ICPPASTTemplateDeclaration)node.getParent().getParent()).getFileLocation());
		}
		else {
			dico.addSourceAnchor(fmx, filename, node.getFileLocation());
		}

		/*Inheritance lastInheritance = null;
		ICPPClassType classBnd = (ICPPClassType)nodeName.resolveBinding();
		for (ICPPBase sup : classBnd.getBases()) {
			IBinding supBnd = sup.getBaseClass();
			if (supBnd instanceof ICPPClassType) {
				try {
					IIndexName[] supNames = index.findDeclarations(supBnd);
					if (supNames.length > 0) {
						eu.synectique.verveine.core.gen.famix.Class supFmx;
						supFmx = dicoDef.ensureClass(supNames[0].getFileLocation().getFileName(), supNames[0].getNodeOffset(), sup.getClassDefinitionName().toString(), /*owner* /null);
						lastInheritance = dico.ensureFamixInheritance(supFmx, fmx, lastInheritance);
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}*/

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	/**
	 * Visiting a method or function declaration
	 */
	@Override
	protected int visit(IASTFunctionDeclarator node) {
		IASTName nodeName;
		IIndexBinding bnd = null;
		BehaviouralEntity fmx = null;

		nodeName = node.getName();
		tracer.msg("IASTFunctionDeclarator: "+nodeName);

		try {
			bnd = index.findBinding(nodeName);
		} catch (CoreException e) {
			System.err.println("error getting index");
			e.printStackTrace();
		}

		if (bnd == null) {
			return PROCESS_SKIP;
		}

		if (bnd instanceof ICPPMethod) {   // C++ method
			fmx = dico.ensureFamixMethod(bnd, formatMemberName(nodeName), /*signature*/bnd.toString(), /*owner*/context.topType());
			if ( ((ICPPMethod)bnd).isDestructor() ) {
				((Method)fmx).setKind(CDictionary.DESTRUCTOR_KIND_MARKER);
			}
			if (bnd instanceof ICPPConstructor) {
				((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
			}
		}
		else {                    //   C function ?
			fmx = dico.ensureFamixFunction(bnd, formatMemberName(nodeName), /*signature*/bnd.toString(), /*owner*/(ContainerEntity)context.top());
		}

		fmx.setIsStub(false);

		context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(IASTFunctionDeclarator node) {
		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof BehaviouralEntity) ) {
			context.pop();
		}
		return ASTVisitor.PROCESS_CONTINUE;
	}

	/**
	 * Visiting a method or function definition
	 */
	protected int visit(ICPPASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;

		/*
		 * visit declarator to ensure the method definition and to get the
		 * Famix entity (which will be on the top of the context stack)
		 */
		this.visit(node.getDeclarator());
		fmx = context.topMethod();    // TODO could be a function here ....
		this.leave(node.getDeclarator());  // at least to popup the function/method from the context stack

		if (fmx != null) {
			IASTFileLocation defLoc = node.getFileLocation();
			dico.addSourceAnchor(fmx, defLoc.getFileName(), defLoc);
		}

		// using pushMethod() introduces a difference in the handling of the metrics CYCLO/NOS
		// this behaviour was inherited from VerveineJ and need to be refactored
		this.context.pushMethod((Method) fmx);

		// now visiting the children of the node
		node.getDeclSpecifier().accept(this);

		context.setLastAccess(null);
		context.setLastInvocation(null);
		context.setLastReference(null);
		context.setTopMethodCyclo(0);
		context.setTopMethodNOS(0);
		node.getBody().accept(this);

		if (fmx != null) {
			// don't remember exactly why it has to be that way
			// this was inherited from VerveineJ and requires some refactoring ...
			int nos = context.getTopMethodNOS();
			int cyclo = context.getTopMethodCyclo();
			if (nos > 1) {
				// if there are statements, cyclomatic complexity is off by 1
				// because it started at 0 instead of 1
				cyclo++;
			}
			fmx.setNumberOfStatements(nos);
			fmx.setCyclomaticComplexity(cyclo);
		}

		this.context.pop();

		return PROCESS_SKIP;  // we already visited the children
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return super.leave(node);
	}

	/**
	 * Visiting an attribute or function declaration.
	 * In the AST it could also be a function parameter, but this is treated in {@link #visit(IASTParameterDeclaration)}}
	 */
	@Override
	protected int visit(ICPPASTDeclarator node) {
		IASTName nodeName = null;
		IIndexBinding bnd = null;
		Attribute fmx = null;

		if ( (node.getParent() instanceof IASTSimpleDeclaration) &&
				(node.getParent().getParent() instanceof IASTCompositeTypeSpecifier) ) {
			// OK, it seems to be an Attribute declaration
			nodeName = node.getName();

			try {
				bnd = index.findBinding(nodeName);
			} catch (CoreException e) {
				System.err.println("error getting index");
				e.printStackTrace();
			}

			if (bnd == null) {
				return PROCESS_SKIP;
			}

			fmx = dico.ensureFamixAttribute(bnd, nodeName.toString(), context.topType());

			fmx.setIsStub(false);

			/*
			 * For ICPPASTDeclarator, the location must be that of the parent, i.e. the declaration
			 * For example, in "int a,b;" the declaration starts at "int" whereas there are 2 declarators: a and b
			 */
			dico.addSourceAnchor(fmx, filename, node.getParent().getFileLocation());
		}

		return PROCESS_CONTINUE;
	}


	// UTILITIES ==============================================================================================================================

	protected String formatMemberName(IASTName nodeName) {
		String ret;
		String fullname = nodeName.toString();
		int i;
		
		i = fullname.lastIndexOf(':');
		if (i > 0 ) {
			ret = fullname.substring(i+1);
		}
		else {
			ret = fullname;
		}

		return ret;
	}

	public void setVisitHeaders(boolean visitHeaders) {
		this.visitHeaders = visitHeaders;
	}

	private boolean checkHeader(ITranslationUnit tu) {
		if (visitHeaders) {
			return (tu.getElementName().indexOf(".h") >= 0);
		}
		else {
			return (tu.getElementName().indexOf(".h") == -1);
		}
	}

}
