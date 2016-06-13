package eu.synectique.verveine.extractor.visitors;

import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IInclude;
import org.eclipse.cdt.core.model.IParent;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.StubBinding;

/**
 * The superclass of all visitors. These visitors visit an AST to create FAMIX entities.<BR>
 * An important function of this abstract class is to dispatch more finely the visits than what CDT normally do. 
 * This visitor also merges two APIs: visit methods on AST (ASTVisitor) and visit methods on ICElements (ICElementVisitor).
 */
public abstract class AbstractVisitor extends ASTVisitor implements ICElementVisitor {

	/** 
	 * A dictionary allowing to recover created FAMIX Entities
	 */
	protected CDictionary dico;

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack context;

	protected ITracer tracer = new NullTracer();  // no tracing by default

	/** name of the current file (TranslationUnit) being visited
	 */
	protected String filename;

	/**
	 * CDT index to resolve symbols
	 */
	protected IIndex index;

	/**
	 * A variable used in many visit methods to store the name of the current node
	 */
	protected IASTName nodeName;

	/**
	 * A variable used in many visit methods to store the binding of the current node
	 */
	protected /*IIndex*/IBinding bnd;

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

	// CONSTRUCTOR ==========================================================================================================================

	public AbstractVisitor(CDictionary dico, IIndex index) {
			super(/*visitNodes*/true);
	    /* fine-tuning if visitNodes=false
	    shouldVisitDeclarations = true;
	    shouldVisitEnumerators = true;
	    shouldVisitProblems = true;
	    shouldVisitTranslationUnit = true;
	    shouldVisit... */
		this.index = index;
	    this.dico = dico;
	}


	// VISITING METODS ON ICELEMENT HIERARCHY ==============================================================================================

	@Override
	public boolean visit(ICElement elt) {
		switch (elt.getElementType()) {
		case ICElement.C_PROJECT:
			visit( (ICProject) elt);
			break;
		case ICElement.C_CCONTAINER:
			visit( (ICContainer) elt);
			break;
		case ICElement.C_UNIT:
			visit( (ITranslationUnit) elt);
			break;
		case ICElement.C_INCLUDE:
			visit( (IInclude) elt);
			break;

		default:
			//  don't know what it is, don't know what to do with it
		}
		return false;
	}

	public void visit(ICProject project) {
		visitChildren(project);
	}

	public void visit(ICContainer cont) {
		visitChildren(cont);
	}

	public void visit(IInclude project) {
	}

	public void visit(ITranslationUnit elt) {
		if (checkHeader(elt)) {
			this.tracer.up("ITranslationUnit: "+elt.getElementName());
			context = new EntityStack();    // "reseting" context
			try {
				visitChildren(elt);

				this.filename = elt.getFile().getRawLocation().toString();
				elt.getAST(index, ITranslationUnit.AST_CONFIGURE_USING_SOURCE_CONTEXT | ITranslationUnit.AST_SKIP_INDEXED_HEADERS).accept(this);
				this.filename = null;
			} catch (CoreException e) {
				System.err.println("*** Got CoreException (\""+ e.getMessage() +"\") while getting AST of "+ elt.getElementName() );
			}
		}

		this.tracer.down();
	}

	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(IASTDeclaration node) {
		if (node instanceof IASTSimpleDeclaration) {
			return visit((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		else if (node instanceof ICPPASTTemplateDeclaration) {
			return visit((ICPPASTTemplateDeclaration)node);
		}
		//else ICPPASTUsingDirective, ...

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof IASTSimpleDeclaration) {
			return leave((IASTSimpleDeclaration)node);
		}
		else if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		else if (node instanceof ICPPASTTemplateDeclaration) {
			return leave((ICPPASTTemplateDeclaration)node);
		}
		//else ICPPASTUsingDirective, ...

		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclarator node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because:
		 * ICPPASTFunctionDeclarator is a sub-interface of IASTFunctionDeclarator
		 * IASTFunctionDeclarator is a sub-interface of ICPPASTDeclarator
		 * ******************************************************************************************** */
		if (node instanceof ICPPASTFunctionDeclarator) {
			return this.visit((ICPPASTFunctionDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			return this.visit((IASTFunctionDeclarator)node);
		}
		else if (node instanceof ICPPASTDeclarator) {
			return this.visit((ICPPASTDeclarator)node);
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclarator node) {
		/* ********************************************************************************************
		 * BE CAREFULL: The order of the tests is important because:
		 * ICPPASTFunctionDeclarator is a sub-interface of IASTFunctionDeclarator
		 * IASTFunctionDeclarator is a sub-interface of ICPPASTDeclarator
		 * ******************************************************************************************** */
		if (node instanceof ICPPASTFunctionDeclarator) {
			return this.leave((ICPPASTFunctionDeclarator)node);
		}
		else if (node instanceof IASTFunctionDeclarator) {
			return this.leave((IASTFunctionDeclarator)node);
		}
		else if (node instanceof ICPPASTDeclarator) {
			return this.leave((ICPPASTDeclarator)node);
		}

		if (node instanceof IASTFunctionDeclarator) {
			return this.leave((IASTFunctionDeclarator)node);
		}
		return super.leave(node);
	}

	@Override
	public int visit(IASTDeclSpecifier node) {
		if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> struct/union
			return this.visit((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> class
			return this.visit((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTElaboratedTypeSpecifier) {
			return this.visit((IASTElaboratedTypeSpecifier)node);
		}
		else if (node instanceof IASTSimpleDeclSpecifier) {
			return this.visit((IASTSimpleDeclSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			return this.visit((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			return this.visit((ICPPASTNamedTypeSpecifier)node);
		}
		else {
			// TODO missing subtypes?
		}

		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclSpecifier node) {
		if (node instanceof ICASTCompositeTypeSpecifier) {
			// -> struct/union
			return this.leave((ICASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof ICPPASTCompositeTypeSpecifier) {
			// -> class
			return this.leave((ICPPASTCompositeTypeSpecifier)node);
		}
		else if (node instanceof IASTEnumerationSpecifier) {
			// -> enum
			return this.leave((IASTEnumerationSpecifier)node);
		}
		else if (node instanceof ICPPASTNamedTypeSpecifier) {
			// -> typedef
			return this.leave((ICPPASTNamedTypeSpecifier)node);
		}

		return super.leave(node);
	}

	@Override
	public int visit(IASTInitializer node) {
		if (node instanceof ICPPASTConstructorChainInitializer) {
			return visit( (ICPPASTConstructorChainInitializer)node );
		}
		else if (node instanceof ICPPASTConstructorInitializer) {
			return visit( (ICPPASTConstructorInitializer)node );
		}
		return super.visit(node);
	}

	@Override
	public int visit(IASTExpression node) {
		if (node instanceof IASTFieldReference) {
			return visit((IASTFieldReference)node);
		}
		else if (node instanceof IASTIdExpression) {
			return visit((IASTIdExpression)node);
		}
		else if (node instanceof ICPPASTNewExpression) {
			return visit((ICPPASTNewExpression)node);
		}
		else if (node instanceof IASTFunctionCallExpression) {
			return visit((IASTFunctionCallExpression)node);
		}
		else if (node instanceof IASTUnaryExpression) {
			return visit((IASTUnaryExpression)node);   // to check whether this is an assignement
		}
		else if (node instanceof IASTBinaryExpression) {
			return visit((IASTBinaryExpression)node);   // to check whether this is an assignement
		}
		else if (node instanceof IASTLiteralExpression) {
			return visit((IASTLiteralExpression)node);
		}

		return super.visit(node);
	}
	

	// ADDITIONAL VISITING METODS ON AST =======================================================================================================

	protected int visit(IASTSimpleDeclaration node) {
		if (node.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_typedef) {
			// this is a typedef, so define a FamixType with the declarator(s)
			for (IASTDeclarator declarator : node.getDeclarators()) {

				nodeName = declarator.getName();

				tracer.msg("IASTSimpleDeclaration (typedef):"+nodeName.toString());

				bnd = getBinding(nodeName);

				if (bnd == null) {
					// create one anyway, assume this is a function
					bnd = StubBinding.getInstance(Function.class, dico.mooseName(getTopCppNamespace(), nodeName.toString()));
				}

				handleTypedef(node);
			}
			
			return PROCESS_SKIP;  // typedef already handled
		}
		return PROCESS_CONTINUE;
	}

	/**
	 * Call back method from {@link visit(IASTSimpleDeclaration)}
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 */
	protected void handleTypedef(IASTSimpleDeclaration node) { }

	protected int leave(IASTSimpleDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTFunctionDeclarator node) {
		bnd = null;
		nodeName = node.getName();
		tracer.msg("ICPPASTFunctionDeclarator: "+nodeName);

		bnd = getBinding(nodeName);

		if (bnd == null) {
			NamedEntity parent = null;
			String behavName;
			// create one anyway, function or method?
			
			if (isFullyQualified(nodeName)) {
				int i = nodeName.toString().lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
				parent = resolveOrNamespace(nodeName.toString().substring(0, i));
			}
			else {
				parent = context.top();
			}
			// for behavioural, we put the full signature in the key to have better chance of recovering it
			SignatureBuilderVisitor sigVisitor = new SignatureBuilderVisitor(dico);
			node.accept(sigVisitor);
			behavName = sigVisitor.getSignature();

			if (parent instanceof eu.synectique.verveine.core.gen.famix.Class) {
				bnd = StubBinding.getInstance(Method.class, dico.mooseName( (eu.synectique.verveine.core.gen.famix.Class)parent, behavName ));
			}
			else {
				bnd = StubBinding.getInstance(Function.class, dico.mooseName((ContainerEntity) parent, behavName));
			}
		}

		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTFunctionDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTDeclarator node) {
		bnd = null;
		nodeName = null;

		if ( (node.getParent() instanceof IASTSimpleDeclaration) &&
			 (node.getParent().getParent() instanceof IASTCompositeTypeSpecifier) &&
			 ( ((IASTSimpleDeclaration)node.getParent()).getDeclSpecifier().getStorageClass() != IASTDeclSpecifier.sc_typedef)  ) {
			// this is an Attribute declaration, get it back
			nodeName = node.getName();

			bnd = getBinding(nodeName);

			if (bnd == null) {
				bnd = StubBinding.getInstance(Attribute.class, dico.mooseName(context.topType(), nodeName.toString()));
			}
		}

		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTDeclarator node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		bnd = null;
		nodeName = node.getName();

		tracer.msg("ICPPASTCompositeTypeSpecifier without name");

		tracer.up("ICPPASTCompositeTypeSpecifier:"+nodeName.toString());

		bnd = getBinding(nodeName);

		if (bnd == null) {
			// create one anyway
			if (isFullyQualified(nodeName)) {
				Namespace parent = recursiveEnsureParentNamespace(nodeName);
				bnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName(parent, simpleName(nodeName)));
			}
			else {
				bnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName(getTopCppNamespace(), nodeName.toString()));
			}
		}

		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	public int visit(IASTElaboratedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}

	public int visit(IASTSimpleDeclSpecifier node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTTemplateDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int leave(ICPPASTTemplateDeclaration node) {
		return PROCESS_CONTINUE;
	}

	public int visit(IASTParameterDeclaration node) {
		bnd = null;
		nodeName = node.getDeclarator().getName();

		if (nodeName.toString().equals("")) {
			// case of a "mth(void)" declaration, seen as a parameter with no name
			// Additionally (to be on the safe side) could check that:
			//   node.getDeclSpecifier() instanceof  IASTSimpleDeclSpecifier
			//   ((IASTSimpleDeclSpecifier) node.getDeclSpecifier()).getType() == IASTSimpleDeclSpecifier.t_void
			return PROCESS_SKIP;
		}

		bnd = getBinding(nodeName);

		if (bnd == null) {
			// create one anyway
			bnd = StubBinding.getInstance(Parameter.class, dico.mooseName(context.topBehaviouralEntity(), nodeName.toString()));
		}
		return PROCESS_CONTINUE;
	}

	public int leave(IASTParameterDeclaration node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFunctionCallExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTNewExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTUnaryExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTBinaryExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTLiteralExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTFieldReference node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(IASTIdExpression node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTConstructorChainInitializer node) {
		return PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTConstructorInitializer node) {
		return PROCESS_CONTINUE;
	}

	// NAME RESOLUTION UTILITIES & STUB CREATION ===========================================================================

	/**
	 * Search for a Namespace or Class at top level
	 * @return NamedEntity found or null if none match
	 */
	private ContainerEntity findTopLevel(String name) {
		for (Type cl : dico.getEntityByName(Type.class, name)) {
			return cl;   // return the 1st type found (if any)
		}
		for (Namespace ns : dico.getEntityByName(Namespace.class, name)) {
			return ns;   // return the 1st namespace found (if any)
		}
		return null;
	}

	/**
	 * Search for a name within the scope of a ContainerEntity.
	 * In the case of looking for a function, name is actually a signature.
	 * @return NamedEntity found or null if none match
	 */
	public NamedEntity findInLocals(String name, ContainerEntity context) {
		for (eu.synectique.verveine.core.gen.famix.Type child : context.getTypes()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		for (Function child : context.getFunctions()) {
			if (child.getSignature().equals(name)) {
				return child;
			}
		}

		return null;
	}

	/**
	 * Search for a name within the scope of a BehaviouralEntity.
	 * @return NamedEntity found or null if none match
	 */
	public NamedEntity findInLocals(String name, BehaviouralEntity context) {		
		for (StructuralEntity child : context.getLocalVariables()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		for (StructuralEntity child : context.getParameters()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		// if not found call the "super" (by casting the variable)
		return findInLocals(name, (ContainerEntity)context);
	}

	/**
	 * Search for a name within the scope of a Type.
	 * In the case of looking for a method, name is actually a signature.
	 * @return NamedEntity found or null if none match
	 */
	public NamedEntity findInLocals(String name, Type context) {		

		for (Attribute child : context.getAttributes()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		for (Method child : context.getMethods()) {
			if (child.getSignature().equals(name)) {
				return child;
			}
		}

		// if not found call the "super" (by casting the variable)
		return findInLocals(name, (ContainerEntity)context);
	}

	/**
	 * Search for a name within the scope of a context.
	 * @return NamedEntity found or null if none match
	 */
	public NamedEntity findInLocals(String name, ScopingEntity context) {		
		for (StructuralEntity child : context.getGlobalVariables()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		for (ScopingEntity child : context.getChildScopes()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		// if not found call the "super" (by casting the variable)
		return findInLocals(name, (ContainerEntity)context);
	}

	/**
	 * Search for a name within the scope of a context.
	 * In the case of looking for a behavioural, name is actually a signature.
	 * If cannot find it and recursive is <code>true</code>, looks in the scope of parent context.
	 * This is a dispatcher method that calls the correct methods from the type of the second parameter
	 * @return NamedEntity found or null if none match
	 */
	public NamedEntity findInParent(String name, NamedEntity context, boolean recursive) {
		NamedEntity found = null;

		if (context == null) {
			return findTopLevel(name);
		}
		else if (context instanceof BehaviouralEntity) {
			found = findInLocals(name, (BehaviouralEntity)context);
		}
		else if (context instanceof ScopingEntity) {
			found = findInLocals(name, (ScopingEntity)context);
		}
		else if (context instanceof Type) {
			found = findInLocals(name, (Type)context);
		}
		else {
			// non ContainerEntity, should never happen
			return null;
		}
		
		if (found != null) {
			return found;
		}

		if (recursive) {
			return findInParent(name, context.getBelongsTo(), recursive);
		}
		else {
			return null;
		}
	}

	/**
	 * Returns the higher-most namespace in the C++ sense on the EntityStack
	 * C++ namespaces we are interested in are: methods, classes, namespaces
	 * @return null if could not find a C++ namespace
	 */
	protected ContainerEntity getTopCppNamespace() {
		Stack<NamedEntity> tmp = new Stack<NamedEntity>();
		NamedEntity top;
		
		top = context.pop();
		tmp.push(top);
		while ( ! ((top == null) ||
				   (top instanceof Method) ||
				   (top instanceof eu.synectique.verveine.core.gen.famix.Class) ||
				   (top instanceof Namespace) )) {
			top = context.pop();
			tmp.push(top);
		}
		
		while (! tmp.empty()) {
			context.push( tmp.pop());
		}

		return (ContainerEntity) top;
	}

	/**
	 * Creates recursively namespaces from a fully qualified name.
	 * The last member of the name is not considered (i.e. a::b::c will yield Namespaces a and a::b)
	 */
	protected Namespace recursiveEnsureParentNamespace(IASTName name) {
		return recursiveEnsureParentNamespace(name.toString());
	}

	protected Namespace recursiveEnsureParentNamespace(String name) {
		int i;
		i = name.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		
		if (i > 0) {
			return recursiveEnsureNamespace(name.substring(0, i));
		}
		else {
			return null;
		}
	}

	/**
	 * Creates recursively namespaces from a fully qualified name.
	 * Assumes the fully qualified name is referent to the root of namespace and not within the current namespace
	 * This means we do not consider the context
	 */
	protected Namespace recursiveEnsureNamespace(String name) {
		int i;
		String namespaceName;
		Namespace parent=null;
		
		i = name.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		if (i > 0) {
			namespaceName = name.substring(i+2);  // could also use simpleName(name)
			parent = recursiveEnsureNamespace(name.substring(0, i));
		}
		else {
			namespaceName = name;
		}

		bnd = StubBinding.getInstance(Namespace.class, dico.mooseName(parent, namespaceName));
		
		return dico.ensureFamixNamespace(bnd, namespaceName, parent);
	}

	/**
	 * Tries to find an entity within the current context, from it's fully qualified name.
	 * If not found, assume it is a Namespace and creates it.
	 */
	protected NamedEntity resolveOrNamespace( String name) {
		NamedEntity tmp;

		// solves the case of a fully qualified name containing only one component (no "::") -----
		if (! isFullyQualified(name)) {
			tmp = findInParent(name, context.top(), /*recursive*/true);

			if (tmp == null) {
				// create as a stub
				bnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity)null, name));
				tmp = dico.ensureFamixNamespace(bnd, name, /*parent*/null);
			}
			return tmp;
		}
		
		// case of a fully qualified name composed of several names -----

		NamedEntity parent = null;
		String str = name;
		int i = name.indexOf(CDictionary.CPP_NAME_SEPARATOR);  // TODO should use String.split() instead of String.substrings()

		// looks for the first component in the fully qualified name
		// it can be in any scope starting from context.top() up to toplevel	
		tmp = findInParent(name.substring(0, i), context.top(), /*recursive*/true);

		// try to find the next component(s) within the one already found (search is no longer recursive in the stack of contexts)
		if (tmp != null) {
			str = name.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
			i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);

			while ( (tmp != null) && (i > 0) ) {
				parent = tmp;
				tmp = findInParent(str.substring(0, i), parent, /*recursive*/false);  // Note: not recursive, we must find in the parent
				str = str.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
				i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);
			}

			// look for the last component in the fully qualified name
			if (tmp != null) {
				parent = tmp;
				tmp = findInParent(str, parent, /*recursive*/false);
				if (tmp != null) {
					return tmp;
				}
			}
		}


		// here, we are sure that the first remaining component in "str" was not found
		// it is possibly followed by other components

		// create last components (not found) as namespaces
		while (i > 0) {
			bnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity) parent, str.substring(0, i)));
			parent = dico.ensureFamixNamespace(bnd, str.substring(0, i), (ScopingEntity) parent);

			str = str.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
			i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);
		}
	
		// and finally the last composant of the fully qualified name
		bnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity) parent, str));
		tmp = dico.ensureFamixNamespace(bnd, str, (ScopingEntity) parent);

		return tmp;
	}

	/**
	 * Ensures a stub class and its parent (a namespace).
	 * Deals with fully qualified class name and not qualified class name (parent is current namespace in this case)
	 */
	protected eu.synectique.verveine.core.gen.famix.Class ensureStubClassInNamespace(String name) {
		IBinding supBnd;
		ContainerEntity parent;

		if (isFullyQualified(name)) {
			parent = recursiveEnsureParentNamespace(name);
			name = simpleName(name);
		}
		else {
			parent = getTopCppNamespace();
		}

		supBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName(parent, name));
		return dico.ensureFamixClass(supBnd, name, parent);
	}

	protected BehaviouralEntity makeStubBehavioural(String name, int nbArgs, boolean isMethod) {
		BehaviouralEntity fmx;
		String stubSig =  mkStubSig(name, nbArgs);
		if (isMethod) {
			fmx = dico.ensureFamixMethod(/*key*/StubBinding.getInstance(Method.class, name+"__"+nbArgs), name, stubSig, /*container*/null);
		}
		else {
			fmx = dico.ensureFamixFunction(/*key*/StubBinding.getInstance(Function.class, name+"__"+nbArgs), name, stubSig, /*container*/null);
		}
		fmx.setNumberOfParameters(nbArgs);
		// there are 2 ways to get the number of parameters of a BehaviouralEntity: getNumberOfParameters() and numberOfParameters()
		// the first returns the attribute numberOfParameters (set here), the second computes the size of parameters
		return fmx;
	}

	/**
	 * Forges a signature for stub BehaviouralEntities
	 * @return "name(&lt;parameters&gt;)" where parametres are substitued by "_" 
	 */
	protected String mkStubSig(String name, int nbParam) {
		String sig = name + "(";
		for (int i=0; i < nbParam-1; i++) {
			sig += "_," ;
		}
		if (nbParam > 0) {
			sig += "_)" ;
		}
		else {
			sig += ")" ;
		}
		return sig;
	}

/*
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
			for (@SuppressWarnings("unused") IASTPointerOperator pointOp : params[p].getDeclarator().getPointerOperators()) {
				if (t.charAt(0) != '*') {
					return false;
				}
				t = t.substring(1).trim();
			}
			p++;
		}
		return true;
	}
*/

	// UTILITIES ======================================================================================================

	private void visitChildren(IParent elt) {
		try {
			for (ICElement child : elt.getChildren()) {
				child.accept(this);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Try to get some binding from a IASTName.
	 * There are two possible way to get bindings: through the Index and by resolveBinding.
	 * but the second may return different bindings for the same entity in different locations
	 * @return a binding or null if none found
	 */
	protected IBinding getBinding(IASTName name) {
		IBinding bnd = null;
		if (name == null) {
			return null;
		}
		try {
			bnd = index.findBinding(name);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return bnd;
	}
	
	protected boolean isFullyQualified(IASTName name) {
		return isFullyQualified(name.toString());
	}

	protected boolean isFullyQualified(String name) {
		return name.indexOf(CDictionary.CPP_NAME_SEPARATOR) > 0;
	}

	/**
	 * Returns the last part of a fully qualified name
	 */
	protected String simpleName(IASTName name) {
		
		return simpleName(name.toString());
	}

	protected String simpleName(String name) {
		String str = name.toString(); 
		int i = str.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		if (i < 0) {
			return name;
		}
		else {
			return str.substring(i+2);
		}
	}

	protected boolean isMethodBinding(IBinding bnd) {
		if (bnd instanceof ICPPMethod) {
			return true;
		}
		if ( (bnd instanceof StubBinding) && ( ((StubBinding)bnd).getEntityClass().equals(Method.class.getName()) ) ) {
			return true;
		}
		return false;
	}

	protected boolean isConstructorBinding(IBinding bnd) {
		if (bnd instanceof ICPPConstructor) {
			return true;
		}
		if (bnd instanceof StubBinding) {
			String className;
			String mthName;
			String fullName = ((StubBinding)bnd).getEntityName();
			int i;
			// name of the method, equivalent to 'simpleName()' but we needed the index to find the className
			i = fullName.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
			mthName = fullName.substring(i+2);
			// name of the class
			fullName = fullName.substring(0, i);
			className = simpleName(fullName);
			// comparing them
			return className.equals(mthName);
		}
		return false;
	}

	protected boolean isDestructorBinding(IBinding bnd) {
		if ( (bnd instanceof ICPPMethod) && (((ICPPMethod)bnd).isDestructor()) ) {
			return true;
		}
		if (bnd instanceof StubBinding) {
			// simplified test. Could look at the name of the class as in isConstructorBinding(bnd)
			return simpleName(((StubBinding)bnd).getEntityName()).charAt(0) == '~';
		}
		return false;
	}

	public void setVisitHeaders(boolean visitHeaders) {
		this.visitHeaders = visitHeaders;
	}

	protected boolean checkHeader(ITranslationUnit tu) {
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

}
