package eu.synectique.verveine.extractor.visitors;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
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
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.CppEntityStack;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.ITracer;
import eu.synectique.verveine.extractor.utils.NullTracer;
import eu.synectique.verveine.extractor.utils.StubBinding;

public abstract class AbstractVisitor extends AbstractDispatcherVisitor {

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected CppEntityStack context;

	protected ITracer tracer = new NullTracer();  // no tracing by default

	/** name of the current file (TranslationUnit) being visited
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


	public AbstractVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}

	@Override
	public void visit(ITranslationUnit elt) {
		context = new CppEntityStack();
		this.filename = elt.getFile().getRawLocation().toString();
		super.visit(elt);
	}

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		Namespace fmx;

		nodeBnd = getBinding(node.getName());

		fmx = (Namespace) dico.getEntityByKey(nodeBnd);

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		return super.leave(node);
	}

	/*
	 * Visiting a class definition to get its key (IBinding) associated with the famix type entity
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		nodeName = node.getName();
		nodeBnd = getBinding(nodeName);

		if (nodeBnd == null) {
			nodeBnd = mkStubKey(nodeName, Class.class);
		}

		return PROCESS_CONTINUE;
	}

	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		nodeBnd = null;
		nodeName = node.getName();
		tracer.msg("ICPPASTFunctionDeclarator: "+nodeName);

		nodeBnd = getBinding(nodeName);

		if (nodeBnd == null) {
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
			// for behavioral, we put the full signature in the key to have better chance of recovering it
			SignatureBuilderVisitor sigVisitor = new SignatureBuilderVisitor(dico);
			node.accept(sigVisitor);
			behavName = sigVisitor.getFullSignature(node);

			if (parent instanceof eu.synectique.verveine.core.gen.famix.Class) {
				nodeBnd = StubBinding.getInstance(Method.class, dico.mooseName( (eu.synectique.verveine.core.gen.famix.Class)parent, behavName ));
			}
			else {
				nodeBnd = StubBinding.getInstance(Function.class, dico.mooseName((ContainerEntity) parent, behavName));
			}
		}

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

		nodeBnd = getBinding(nodeName);
		if (nodeBnd == null) {
			nodeBnd = StubBinding.getInstance(Parameter.class, dico.mooseName(context.topBehaviouralEntity(), nodeName.toString()));
		}

		return PROCESS_CONTINUE;
	}

	// NAME RESOLUTION UTILITIES & STUB CREATION ===========================================================================

	protected <T extends NamedEntity> IBinding mkStubKey(IASTName name, java.lang.Class<T> entityType) {
		ContainerEntity parent = null;
		String simpleName = null;
		if (isFullyQualified(name)) {
			parent = getParentOfFullyQualifiedName(name);
			simpleName = simpleName(name);
		}
		else {
			parent = context.getTopCppNamespace();
			simpleName = name.toString();
		}
		return StubBinding.getInstance(entityType, dico.mooseName(parent, simpleName));
	}

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
	 * Creates recursively namespaces from a fully qualified name.
	 * The last member of the name is not considered (i.e. a::b::c will yield Namespaces a and a::b)
	 */
	protected Namespace getParentOfFullyQualifiedName(IASTName name) {
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

		nodeBnd = StubBinding.getInstance(Namespace.class, dico.mooseName(parent, namespaceName));
		
		return dico.ensureFamixNamespace(nodeBnd, namespaceName, parent);
	}

	/**
	 * Tries to find an entity within the current context, from it's fully qualified name.
	 * If not found, tries to create it with the correct type (Namespace being the default)
	 */
	protected NamedEntity resolveOrNamespace( String name) {
		NamedEntity tmp;

		// solves the case of a fully qualified name containing only one component (no "::") -----
		if (! isFullyQualified(name)) {
			tmp = findInParent(name, context.top(), /*recursive*/true);

			if (tmp == null) {
				// create as a stub
				nodeBnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity)null, name));
				tmp = dico.ensureFamixNamespace(nodeBnd, name, /*parent*/null);
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
			nodeBnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity) parent, str.substring(0, i)));
			parent = dico.ensureFamixNamespace(nodeBnd, str.substring(0, i), (ScopingEntity) parent);

			str = str.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
			i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);
		}

		// and finally the last composant of the fully qualified name
		// first compute the type of this composant
		// rule of the thumb: if parent is not a namespace (class or method), then we create a Class
		if ( (parent == null) || (parent instanceof Namespace) ) {
			nodeBnd = StubBinding.getInstance(Namespace.class, dico.mooseName((ContainerEntity) parent, str));
			tmp = dico.ensureFamixNamespace(nodeBnd, str, (ScopingEntity) parent);
		}
		else {
			nodeBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName((ContainerEntity) parent, str));
			tmp = dico.ensureFamixClass(nodeBnd, str, (ContainerEntity) parent);
		}

		return tmp;
	}

	/**
	 * Ensures a (stub) type/class and its parent (a namespace).
	 * Deals with fully qualified class name and not qualified class name
	 */
	protected Type resolveOrClass(String name) {
		ContainerEntity parent = null;
		Type typ;

		if (isFullyQualified(name)) {
			parent = recursiveEnsureParentNamespace(name);
			name = simpleName(name);
		}
		
		// sometimes the "class" happens to have been created as a type before ...
		typ = (Type) findInParent(name, parent, /*recursive*/false);
		
		if (typ == null) {
			IBinding classBnd = StubBinding.getInstance(eu.synectique.verveine.core.gen.famix.Class.class, dico.mooseName(parent, name));
			typ = dico.ensureFamixClass(classBnd, name, parent);
		}
		return typ;
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

	// UTILITIES ======================================================================================================

	protected boolean declarationIsTypedef(IASTSimpleDeclaration node) {
		return (node.getDeclSpecifier().getStorageClass() == IASTDeclSpecifier.sc_typedef);
	}

	protected boolean nodeParentIsClass(IASTNode node) {
		return node.getParent() instanceof IASTCompositeTypeSpecifier;
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

	protected boolean isStubBinding(IBinding bnd) {
		return (bnd instanceof StubBinding);
	}

	protected boolean isMethodBinding(IBinding bnd) {
		if (bnd instanceof ICPPMethod) {
			return true;
		}
		if ( isStubBinding(bnd) && ( ((StubBinding)bnd).getEntityClass().equals(Method.class.getName()) ) ) {
			return true;
		}
		return false;
	}

	protected boolean isConstructor(BehaviouralEntity fmx) {
		if ( ! (fmx instanceof Method) ) {
			return false;
		}
		if ( ((Method)fmx).getKind() == null ) {
			return false;
		}
		return ((Method)fmx).getKind().equals(CDictionary.CONSTRUCTOR_KIND_MARKER);
	}

	protected boolean isDestructor(BehaviouralEntity fmx) {
		if ( ! (fmx instanceof Method) ) {
			return false;
		}
		if ( ((Method)fmx).getKind() == null ) {
			return false;
		}
		return ((Method)fmx).getKind().equals(CDictionary.DESTRUCTOR_KIND_MARKER);
	}

	protected boolean isConstructorBinding(IBinding bnd) {
		if (bnd instanceof ICPPConstructor) {
			return true;
		}
		if (isStubBinding(bnd)) {
			String fullName = ((StubBinding)bnd).getEntityName();
			int i;
			// remove parameters from the name
			i = fullName.indexOf('(');

//System.err.println("isConstructorBinding // "+fullName);
	        fullName = fullName.substring(0, i);
			String[] parts = fullName.split(CDictionary.CPP_NAME_SEPARATOR);
			
			i = parts.length;
			if (i < 2) {
				return false; // not a qualified name, can be a simple, top level, function
			}
			return parts[i-2].equals(parts[i-1]);  // className (second to last part) = methName (last part) ?
		}
		return false;
	}

	protected boolean isDestructorBinding(IBinding bnd) {
		if ( (bnd instanceof ICPPMethod) && (((ICPPMethod)bnd).isDestructor()) ) {
			return true;
		}
		if (isStubBinding(bnd)) {
			// simplified test. Could look at the name of the class as in isConstructorBinding(bnd)
			return simpleName(((StubBinding)bnd).getEntityName()).charAt(0) == '~';
		}
		return false;
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

	protected BehaviouralEntity resolveBehaviouralFromName(ICPPASTFunctionDeclarator node, IBinding bnd) {
		String mthSig;
		Type parent;
		BehaviouralEntity fmx;
		if (isMethodBinding(bnd)) {
			/* get method name and parent */
			if (bnd instanceof StubBinding) {
				String fullName = ((StubBinding)bnd).getEntityName();
				if (isFullyQualified(fullName)) {
					mthSig = extractMethodSignature(fullName);
					parent = (Type) resolveOrNamespace(extractMethodParentName(fullName));
				}
				else {
					mthSig = fullName;
					parent = context.topType();
				}
			}
			else {
				mthSig = new SignatureBuilderVisitor(dico).getFullSignature(node);
				if (isFullyQualified(nodeName)) {
					parent = (Type) dico.getEntityByKey( ((ICPPMethod)bnd).getClassOwner() );
				}
				else {
					parent = context.topType();
				}
			}
			
			/* last try to recover method ... */
			fmx = (BehaviouralEntity) findInParent(mthSig, parent, false);
			/* ... or create it */
			if (fmx == null) {
				fmx = dico.ensureFamixMethod(bnd, simpleName(nodeName.toString()), mthSig, /*owner*/parent);
			}
		}
		else {                    //   C function or may be a stub ?
			fmx = dico.ensureFamixFunction(bnd, simpleName(nodeName), new SignatureBuilderVisitor(dico).getFullSignature(node), (ContainerEntity)context.top());
		}
		return fmx;
	}

	protected String extractMethodSignature(String fullname) {
		int i;
		i = fullname.indexOf('(');
		i = fullname.substring(0, i).lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		return fullname.substring(i+CDictionary.CPP_NAME_SEPARATOR.length());
	}

	protected String extractMethodParentName(String fullname) {
		int i;
		i = fullname.indexOf('(');
		fullname = fullname.substring(0, i);
		i = fullname.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		return fullname.substring(0, i);
	}


	/**
	 * Dictionary getter.<BR>
	 * Only intended for subRef visitors to get the same dictionary as their parent visitor
	 */
	public CDictionary getDico() {
		return dico;
	}

	/**
	 * context setter.<BR>
	 * Only intended for subRef visitors to have the same context as their parent visitor
	 */
	public void setContext(CppEntityStack context) {
		this.context = context;
	}

	/**
	 * context getter.<BR>
	 * Only intended for subRef visitors to get the same context as their parent visitor
	 */
	public CppEntityStack getContext() {
		return context;
	}

	/**
	 * Index getter.<BR>
	 * Only intended for subRef visitors to get the same index as their parent visitor
	 */
	public IIndex getIndex() {
		return index;
	}

}
