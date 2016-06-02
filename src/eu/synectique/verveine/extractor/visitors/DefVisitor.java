package eu.synectique.verveine.extractor.visitors;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
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
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.visitors.ref.RefVisitor;

public class DefVisitor extends AbstractVisitor implements ICElementVisitor {

	/**
	 * The file directory being visited at any given time
	 */
	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	/**
	 * A flag to allow visiting separately header files (.h) and source files (.c)
	 * The idea is to visit first the header files
	 */
	protected boolean visitHeaders;

	/**
	 * A set of all unresolved includes so that we report them only once
	 */
	protected Set<String> unresolvedIncludes;

	// CONSTRUCTOR ==========================================================================================================================

	/**
	 * Default constructor for definition pass
	 * @param dico where entities are created
	 * @param index CDT index containing bindings
	 */
	public DefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);

		unresolvedIncludes = new HashSet<String>();
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
			context = new EntityStack();    // "reseting" context
			super.visit(tu);
			tracer.down();
		}
	}

	public void visit(IInclude elt) {
		if (! elt.isResolved()) {
			String includeStr;
			includeStr = elt.isLocal() ? "\"" : "<";
			includeStr += elt.getIncludeName();
			includeStr += elt.isLocal() ? "\"" : ">";
			if (! unresolvedIncludes.contains(includeStr)) {
				unresolvedIncludes.add(includeStr);
				System.err.println("Include not resolved: "+ includeStr);
			}
		}
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		Namespace fmx;
		nodeName = node.getName();
		bnd = null;

		bnd = getBinding(nodeName);
		fmx = dico.ensureFamixNamespace(bnd, nodeName.getLastName().toString(), (Namespace) this.context.top());
		fmx.setIsStub(false);

		this.context.push(fmx);

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		Parameter fmx = null;

		 // get node name and bnd
		if (super.visit(node) == PROCESS_SKIP) {
			return PROCESS_SKIP;
		}

		fmx = dico.ensureFamixParameter(bnd, nodeName.toString(), context.topBehaviouralEntity());
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


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;
		boolean isTemplate;

		// compute nodeName and binding
		super.visit(node);

		isTemplate = (node.getParent().getParent() instanceof ICPPASTTemplateDeclaration);

		if (isTemplate) {
			fmx = dico.ensureFamixParameterizableClass(bnd, nodeName.toString(), (ContainerEntity)context.top());
		}
		else {
			// if node is a stub with a fully qualified name, its parent is not context.top() :-(
			fmx = dico.ensureFamixClass(bnd, nodeName.toString(), (ContainerEntity)context.top());
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

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	/**
	 * Visiting a method or function declaration (i.e. "header" aka signature)
	 */
	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		String mthName;
		BehaviouralEntity fmx = null;

		// compute nodeName and binding
		super.visit(node);

		if (isMethodBinding(bnd)) {
			Type parent;
			
			 // get the class owning the method and the method name
			if (bnd instanceof StubBinding) {
				String fullName = nodeName.toString();
				int i = fullName.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
				if (i > 0) {
					mthName = fullName.substring(i+2);
					parent = (Type) resolveOrNamespace(fullName.substring(0, i));
				}
				else {
					mthName = fullName;
					parent = context.topType();
				}

				fmx = lookForUnboundMethod(parent, mthName, node.getParameters());
			}
			else {
				mthName = simpleName(nodeName);
				parent = context.topType();
			}

			if (fmx == null) {
				fmx = dico.ensureFamixMethod(bnd, mthName, node.getRawSignature(), /*owner*/parent);
				fmx.setNumberOfParameters(node.getParameters().length);
				// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
				// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
			}

			if (isDestructorBinding(bnd)) {
				((Method)fmx).setKind(CDictionary.DESTRUCTOR_KIND_MARKER);
			}
			if (isConstructorBinding(bnd)) {
				((Method)fmx).setKind(Dictionary.CONSTRUCTOR_KIND_MARKER);
			}
		}
		else {                    //   C function or may be a stub ?
			fmx = dico.ensureFamixFunction(bnd, simpleName(nodeName), node.getRawSignature(), /*owner*/(ContainerEntity)context.top());
		}
		fmx.setIsStub(false);  // used to say TRUE if could not find a binding. Not too sure ... 
		fmx.setNumberOfParameters(node.getParameters().length);
		// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters

		this.context.push(fmx);
		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		this.context.pop();

		return PROCESS_SKIP;
	}

	@Override
	protected int leave(ICPPASTFunctionDeclarator node) {
		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof BehaviouralEntity) ) {
			context.pop();
		}
		return ASTVisitor.PROCESS_CONTINUE;
	}

	/**
	 * Call back method from {@link AbstractVisitor#visit(IASTSimpleDeclaration)}
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 */
	@Override
	protected void handleTypedef(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = dico.ensureFamixTypeAlias(bnd, nodeName.toString(), (ContainerEntity)context.top());

		if (! (bnd instanceof StubBinding)) {
			fmx.setIsStub(false);
		}

		fmx.setParentPackage(currentPackage);
	}

	/**
	 * Visiting a method or function definition
	 */
	protected int visit(ICPPASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;
		tracer.up("ICPPASTFunctionDefinition");

		this.visit( (ICPPASTFunctionDeclarator)node.getDeclarator());
		fmx = context.topBehaviouralEntity();
		this.leave((ICPPASTFunctionDeclarator)node.getDeclarator());  // will popup the function/method from the context stack

		if (fmx != null) {
			IASTFileLocation defLoc = node.getFileLocation();
			dico.addSourceAnchor(fmx, defLoc.getFileName(), defLoc);
		}

		// using pushBehaviouralEntity()/pushMethod() introduces a difference in the handling of the stack (for metrics CYCLO/NOS)
		// this behaviour was inherited from VerveineJ and would need to be refactored
		this.context.pushBehaviouralEntity(fmx);

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
		tracer.down();

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
		Attribute fmx = null;

		// compute nodeName and binding
		bnd = null;
		super.visit(node);

		if (bnd != null) {
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

/*	protected String formatMemberName(IASTName nodeName) {
		String ret;
		String fullname = nodeName.toString();
		int i;
		
		i = fullname.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		if (i > 0 ) {
			ret = fullname.substring(i+2);
		}
		else {
			ret = fullname;
		}

		return ret;
	}*/

	public void setVisitHeaders(boolean visitHeaders) {
		this.visitHeaders = visitHeaders;
	}

	private boolean checkHeader(ITranslationUnit tu) {
		String ext;
		int i = tu.getElementName().lastIndexOf('.');
		if (i < 0) {
			return false;    // not a source file
		}
		ext = tu.getElementName().substring(i);
		if (visitHeaders) {
			return ext.startsWith(".h");
		}
		else {
			return (! ext.startsWith(".h") );
		}
	}

	public int nbUnresolvedIncludes() {
		return unresolvedIncludes.size();
	}

	private BehaviouralEntity lookForUnboundMethod(Type parent, String name, ICPPASTParameterDeclaration[] params) {
		for (Method mth : parent.getMethods()) {
			if (mth.getName().equals(name)) {
				if (checkParams(mth, params)) {
					return mth;
				}
			}
		}
		return null;
	}

	private boolean checkParams(Method mth, ICPPASTParameterDeclaration[] params) {
		int i;
		String sig = mth.getSignature();
		i = sig.indexOf('(');
		sig = sig.substring(i+1, sig.length()-1);
		int p = 0;
		for (String t : sig.split(",")) {
			t = t.trim();
			// trying to match the type name
			String typName = params[p].getDeclSpecifier().getRawSignature();
			if (! t.startsWith( typName)) {
				return false;
			}
			t = t.substring(typName.length()).trim();
			// trying to match a pointer
			for (IASTPointerOperator pointOp : params[p].getDeclarator().getPointerOperators()) {
				if (t.charAt(0) != '*') {
					return false;
				}
				t = t.substring(1).trim();
			}
			p++;
		}
		return true;
	}

}
