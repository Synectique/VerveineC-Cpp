package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.CppEntityStack;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.NameResolver;

public abstract class AbstractVisitor extends AbstractDispatcherVisitor {

	/**
	 * Prefix to remove from file names
	 */
	protected String rootFolder;

	/**
	 * An object responsible for resolving names.
	 * This implies keeping track of the current context stack as well as handling the CDT IIndex, finding bindings for names,
	 * or dealing with name (fully-qualified or not)
	 */
	protected NameResolver resolver;

	/**
	 *  name of the current file (TranslationUnit) being visited
	 */
	protected String filename;

	/**
	 * A variable used in many visit methods to store the name of the current node
	 */
	protected IASTName nodeName;

	/**
	 * A variable used in many visit methods to store the binding of the current node
	 */
	protected IBinding nodeBnd;

	/**
	 * FamixSourcedEntity created as a result of a visitor.
	 * This is required to treat it in a parent visit method or a potential parent visitor.
	 * However, return value of Visitors is already codified by {@link ASTVisitor}
	 * (see {@link ASTVisitor#PROCESS_CONTINUE}m {@link ASTVisitor#PROCESS_ABORT}m and {@link ASTVisitor#PROCESS_SKIP}.
	 * This attributes allows to hold "another return value" (together with a getter)
	 */
	protected SourcedEntity returnedEntity;

	/**
	 * A flag to allow visiting separately header files (.h) and source files (.c)
	 * The idea is to visit first the header files
	 */
	protected boolean visitHeaders;

	public AbstractVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index);
		this.rootFolder = rootFolder;
		this.resolver = new NameResolver(dico, index);
	}

	/*
	 * be creful, overriden in some subclasses so that this one is not called
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		setContext(new CppEntityStack());
		this.filename = FileUtil.localized(elt.getFile().getRawLocation().toString(), rootFolder);
		super.visit(elt);
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		Namespace fmx;

		nodeBnd = resolver.getBinding(node.getName());

		fmx = (Namespace) dico.getEntityByKey(nodeBnd);

		getContext().push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		getContext().pop();
		return super.leave(node);
	}

	/*
	 * Visiting a class definition to get its key (IBinding) associated with the famix type entity
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		return this.visit( (IASTCompositeTypeSpecifier)node);
	}

	/*
	 * Visiting a class definition to get its key (IBinding) associated with the famix type entity
	 */
	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		return this.visit((IASTCompositeTypeSpecifier)node);
	}

	protected int visit(IASTCompositeTypeSpecifier node) {
		nodeName = node.getName();
		nodeBnd = resolver.getBinding(nodeName);

		if (nodeBnd == null) {
			nodeBnd = resolver.mkStubKey(nodeName, Class.class);
		}

		return PROCESS_CONTINUE;
	}

	/**
	 * All functions treated equally here
	 * Inheritance hierarchy of function declarator nodes:
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
	 */
	@Override
	protected int visit(IASTFunctionDeclarator node) {
		nodeName = node.getName();
		nodeBnd = resolver.getFunctionBinding(node, nodeName);

		return PROCESS_CONTINUE;
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
		nodeBnd = null;
		nodeName = node.getDeclarator().getName();

		if (nodeName.toString().equals("")) {
			// case of a "mth(void)" declaration, seen as a parameter with no name
			// also happens for fct/method declaration (as opposed to definition) as in: "mth(int,char*)" (no parameter name)
			return PROCESS_SKIP;
		}

		nodeBnd = resolver.getBinding(nodeName);
		if (nodeBnd == null) {
			nodeBnd = resolver.mkStubKey(nodeName.toString(),Parameter.class);
		}

		return PROCESS_CONTINUE;
	}

	protected void visitParameters(IASTNode[] params, BehaviouralEntity fmx) {
		getContext().push(fmx);
		for (IASTNode param : params) {
			param.accept(this);
		}
		getContext().pop();
	}

	@Override
	protected int visit(ICPPASTVisibilityLabel node) {
		return PROCESS_CONTINUE;
	}

	// NAME RESOLUTION UTILITIES & STUB CREATION ===========================================================================

	protected BehaviouralEntity makeStubBehavioural(String name, int nbArgs, boolean isMethod) {
		BehaviouralEntity fmx;
		String stubSig =  resolver.mkStubSig(name, nbArgs);
		if (isMethod) {
			fmx = dico.ensureFamixMethod(/*key*/resolver.mkStubKey(name+"__"+nbArgs, Method.class), name, stubSig, /*container*/null);
		}
		else {
			fmx = dico.ensureFamixFunction(/*key*/resolver.mkStubKey(name+"__"+nbArgs, Function.class), name, stubSig, /*container*/null);
		}
		fmx.setNumberOfParameters(nbArgs);
		// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
		return fmx;
	}

	// UTILITIES ======================================================================================================

	protected boolean declarationIsTypedef(IASTSimpleDeclaration node) {
		return (node.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_typedef);
	}

	protected boolean nodeParentIsClass(IASTNode node) {
		return node.getParent() instanceof IASTCompositeTypeSpecifier;
	}

	public void setVisitHeaders(boolean visitHeaders) {
		this.visitHeaders = visitHeaders;
	}

	protected boolean checkHeader(ITranslationUnit tu) {
		if (visitHeaders) {
			return FileUtil.isHeader(tu);
		}
		else {
			return (! FileUtil.isHeader(tu) );
		}
	}


	protected CppEntityStack getContext() {
		return resolver.getContext();
	}

	protected void setContext(CppEntityStack context) {
		resolver.setContext( context);
	}

}
