package eu.synectique.verveine.extractor.utils;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.visitors.SignatureBuilderVisitor;

/**
 * A library class with several utility methods useful for name resolution.
 */
public class NameResolver {

	private CppEntityStack context;


	/**
	 * CDT index to get AST and resolve symbols
	 */
	protected IIndex index;

	/** 
	 * A dictionary allowing to store created FAMIX Entities
	 * <p>
	 * This does not strictly belongs here, but all sub-classes need it,
	 * so it's simpler to have it in their super-class (this one)
	 */
	protected CDictionary dico;

	public NameResolver(CDictionary dico, IIndex index) {
		super();
		this.dico = dico;
		this.index = index;
	}

	

	public CppEntityStack getContext() {
		return context;
	}

	public void setContext(CppEntityStack context) {
		this.context = context;
	}

	/**
	 * Creates a StubBinding for <code>name</code>. If <code>name</code> is unqualified, defaults to considering it in the current context
	 */
	public <T extends NamedEntity> IBinding mkStubKey(IASTName name, java.lang.Class<T> entityType) {
		return mkStubKey( name.toString(), entityType);
	}

	public <T extends NamedEntity> IBinding mkStubKey(String name, java.lang.Class<T> entityType) {
		return mkStubKey( name, context.getTopCppNamespace(), entityType);
	}

	/**
	 * Creates a StubBinding for <code>name</code>. If <code>name</code> is unqualified, considers it is a child of <code>parent</code>.
	 * If  <code>name</code> is (fully)qualified, ignores <code>parent</code>.
	 */
	protected <T extends NamedEntity> IBinding mkStubKey(IASTName name, ContainerEntity parent, java.lang.Class<T> entityType) {
		return mkStubKey(name, parent, entityType);
	}

	public <T extends NamedEntity> IBinding mkStubKey(String name, ContainerEntity parent, java.lang.Class<T> entityType) {
		String simpleName = null;
		if (isFullyQualified(name)) {
			parent = (ContainerEntity) resolveOrNamespace(nameQualifier(name));
			simpleName = unqualifiedName(name);
		}
		else {
			simpleName = name;
		}
		return StubBinding.getInstance(entityType, dico.mooseName(parent, simpleName));
	}
	
	public boolean isFullyQualified(IASTName name) {
		return isFullyQualified(name.toString());
	}

	protected boolean isFullyQualified(String name) {
		return name.indexOf(CDictionary.CPP_NAME_SEPARATOR) >= 0;
	}

	/**
	 * Returns the last part of a fully qualified name
	 */
	public String unqualifiedName(IASTName name) {
		
		return unqualifiedName(name.toString());
	}

	public String unqualifiedName(String name) {
		String str = name.toString();
		int i = str.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		if (i < 0) {
			return name;
		}
		else {
			return str.substring(i+2);
		}
	}

	/**
	 * "Opposite" of unqualifiedName, returns the qualifying part of a fully qualified name
	 */
	public String nameQualifier(IASTName name) {
		return nameQualifier(name.toString());
	}

	public String nameQualifier(String name) {
		String str = name.toString();
		int i = str.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		if (i < 0) {
			return "";
		}
		else {
			return str.substring(0, i);
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

	public boolean isConstructor(BehaviouralEntity fmx) {
		if ( ! (fmx instanceof Method) ) {
			return false;
		}
		if ( ((Method)fmx).getKind() == null ) {
			return false;
		}
		return ((Method)fmx).getKind().equals(CDictionary.CONSTRUCTOR_KIND_MARKER);
	}

	public boolean isDestructor(BehaviouralEntity fmx) {
		if ( ! (fmx instanceof Method) ) {
			return false;
		}
		if ( ((Method)fmx).getKind() == null ) {
			return false;
		}
		return ((Method)fmx).getKind().equals(CDictionary.DESTRUCTOR_KIND_MARKER);
	}

	public boolean isConstructorBinding(IBinding bnd) {
		if (bnd instanceof ICPPConstructor) {
			return true;
		}
		if (isStubBinding(bnd)) {
			String fullName = ((StubBinding)bnd).getEntityName();
			int i;
			// remove parameters from the name
			i = fullName.indexOf('(');
			if (i > 0) {
				fullName = fullName.substring(0, i);
			}

			String[] parts = fullName.split(CDictionary.CPP_NAME_SEPARATOR);
			
			i = parts.length;
			if (i < 2) {
				return false; // not a qualified name, can be a simple, top level, function
			}
			return parts[i-2].equals(parts[i-1]);  // className (second to last part) = methName (last part) ?
		}
		return false;
	}

	public boolean isDestructorBinding(IBinding bnd) {
		if ( (bnd instanceof ICPPMethod) && (((ICPPMethod)bnd).isDestructor()) ) {
			return true;
		}
		if (isStubBinding(bnd)) {
			// simplified test. Could look at the name of the class as in isConstructorBinding(bnd)
			return unqualifiedName(((StubBinding)bnd).getEntityName()).charAt(0) == '~';
		}
		return false;
	}

	/**
	 * Try to get some binding from a IASTName.
	 * There are two possible way to get bindings: through the Index and by resolveBinding.
	 * but the second may return different bindings for the same entity in different locations
	 * @return a binding or null if none found
	 */
	public IBinding getBinding(IASTName name) {
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
	
	public IBinding getFunctionBinding(IASTFunctionDeclarator node, IASTName name) {
		IBinding bnd;
		
		bnd = getBinding(name);   // generic getBinding method

		if (bnd == null) {
			ContainerEntity parent = null;
			String behavName = computeSignatureFromAST(node);

			// need to find the parent here (although mkStubKey can do it for us)
			// because need to know whether it is a method or a function
			if (isFullyQualified(name)) {
				parent = (ContainerEntity) resolveOrNamespace(nameQualifier(name));
			}
			else {
				parent = context.getTopCppNamespace();
			}

			if (parent instanceof eu.synectique.verveine.core.gen.famix.Class) {
				bnd = mkStubKey( behavName, parent, Method.class);
			}
			else {
				bnd = mkStubKey(behavName, (ContainerEntity) parent, Function.class);
			}
		}
		return bnd;
	}

	/**
	 * Get a behaviouralEntity for the node.
	 * Tries to recover from the binding or the name (does some name resolution based on fully qualified names).
	 * If all fails, will create an entity.
	 */
	public BehaviouralEntity ensureBehavioural(IASTFunctionDeclarator node, IBinding nodeBnd, IASTName nodeName) {
		BehaviouralEntity fmx;

		// just in case this is a definition and we already processed the declaration
		fmx = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);
		// try harder
		if (fmx == null) {
			fmx = ensureBehaviouralFromName(node, nodeBnd, nodeName);
		}
		return fmx;
	}

	public BehaviouralEntity ensureBehaviouralFromName(IASTFunctionDeclarator node, IBinding bnd, IASTName name) {
		String mthSig;
		Type parent;
		BehaviouralEntity fmx;
		if (isMethodBinding(bnd)) {
			// get method name and parent
			if (bnd instanceof StubBinding) {
				String fullname = ((StubBinding)bnd).getEntityName();
				mthSig = extractSignatureFromMethodFullname(fullname);
				parent = getParentTypeFromNameOrContext(fullname);
			}
			else {
				mthSig = computeSignatureFromAST(node);
				parent = getMethodParentFromBindingOrContext(bnd, name);
			}
			
			// last try to recover method ...
			fmx = (BehaviouralEntity) findInParent(mthSig, parent, false);
			// ... create it if failed
			if (fmx == null) {
				fmx = dico.ensureFamixMethod(bnd, unqualifiedName(name.toString()), mthSig, /*owner*/parent);
			}
		}
		else {                    //   C function or may be a stub ?
			fmx = dico.ensureFamixFunction(bnd, unqualifiedName(name), computeSignatureFromAST(node), (ContainerEntity)context.top());
		}
		return fmx;
	}


	/**
	 * Search for a Namespace or Class at top level
	 * @return NamedEntity found or null if none match
	 */
	private ContainerEntity findAtTopLevel(String name) {
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
			return findAtTopLevel(name);
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
	 * Tries to find an entity within the current context, from it's fully qualified name.
	 * If not found, tries to create it with the correct type (Namespace being the default)
	 */
	public NamedEntity resolveOrNamespace( String name) {
		NamedEntity tmp;
		IBinding bnd;

		if (name.equals("")) {
			return null;
		}

		// solves the case of a name containing only one component (no "::") -----
		if (! isFullyQualified(name)) {
			tmp = findInParent(name, context.top(), /*recursive*/true);

			if (tmp == null) {
				// create as a stub at top level. May be should create it in current context?
				bnd = mkStubKey(name, /*container*/null, Namespace.class);
				tmp = dico.ensureFamixNamespace(bnd, name, /*parent*/null);
			}
			return tmp;
		}

		// case of a fully qualified name composed of several names -----

		NamedEntity parent = null;
		String str = name;
		int i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);

		if (i == 0) {
			// special case of names ::google::xyz::www
			str = name.substring(CDictionary.CPP_NAME_SEPARATOR.length());
			i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);
			tmp = findAtTopLevel( str.substring(0, i) );
		}
		else {
			// looks for the first component in the fully qualified name
			// it can be in any scope starting from context.top() up to toplevel	
			tmp = findInParent(name.substring(0, i), context.top(), /*recursive*/true);
		}

		// try to find the next component(s) name(s) within the one already found (search is no longer recursive in the stack of contexts)
		if (tmp != null) {
			str = str.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
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


		// here, we are sure that the last remaining component in "str" was not found
		// it is possibly preceded by other components

		// create last components (not found) as namespaces
		while (i > 0) {
			bnd = mkStubKey(str.substring(0, i), (ContainerEntity)parent, Namespace.class);
			parent = dico.ensureFamixNamespace(bnd, str.substring(0, i), (ScopingEntity) parent);

			str = str.substring(i + CDictionary.CPP_NAME_SEPARATOR.length());
			i = str.indexOf(CDictionary.CPP_NAME_SEPARATOR);
		}

		// and finally the last composant of the fully qualified name
		// first compute the type of this composant
		// rule of the thumb: if parent is not a namespace (probably a class or method), then we create a Class
		// because we can't have a namespace inside a class or method
		if ( (parent == null) || (parent instanceof Namespace) ) {
			bnd = mkStubKey(str, (ContainerEntity) parent, Namespace.class);
			tmp = dico.ensureFamixNamespace(bnd, str, (ScopingEntity) parent);
		}
		else {
			bnd = mkStubKey(str, (ContainerEntity) parent, eu.synectique.verveine.core.gen.famix.Class.class);
			tmp = dico.ensureFamixClass(bnd, str, (ContainerEntity) parent);
		}

		return tmp;
	}

	/**
	 * Ensures a (stub) type/class and its parent (a namespace).
	 * Deals with fully qualified class name and not qualified class name
	 */
	public Type resolveOrClass(String name) {
		ContainerEntity parent = null;
		Type typ;

		if (isFullyQualified(name)) {
			// looks for the parent of the class	
			parent = (ContainerEntity) resolveOrNamespace(nameQualifier(name));
			name = unqualifiedName(name);
		}
		
		// sometimes the "Class" happens to have been created as a Type before, so becareful when casting ...
		// parent can be null and will look at top level
		typ = (Type) findInParent(name, parent, /*recursive*/false);
		
		if (typ == null) {
			IBinding classBnd = mkStubKey(name, parent, eu.synectique.verveine.core.gen.famix.Class.class);
			typ = dico.ensureFamixClass(classBnd, name, parent);
		}
		return typ;
	}

	protected String computeSignatureFromAST(IASTFunctionDeclarator node) {
		String behavName;
		// for behavioral, we put the full signature in the key to have better chance of recovering it
		SignatureBuilderVisitor sigVisitor = new SignatureBuilderVisitor(dico);
		node.accept(sigVisitor);
		behavName = sigVisitor.getSignature();
		return behavName;
	}

	protected String extractSignatureFromMethodFullname(String fullname) {
		if (isFullyQualified(fullname)) {
			int i;
			i = fullname.indexOf('(');
			i = fullname.substring(0, i).lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
			return fullname.substring(i+CDictionary.CPP_NAME_SEPARATOR.length());
		}
		else {
			return fullname;
		}
	}

	public String extractParentNameFromMethodFullname(String fullname) {
		int i;
		i = fullname.indexOf('(');
		if (i > 0) {
			fullname = fullname.substring(0, i);
		}

		i = fullname.lastIndexOf(CDictionary.CPP_NAME_SEPARATOR);
		return fullname.substring(0, i);
	}

	protected Type getMethodParentFromBindingOrContext(IBinding bnd, IASTName name) {
		Type parent;
		if (isFullyQualified(name)) {
			parent = (Type) dico.getEntityByKey( ((ICPPMethod)bnd).getClassOwner() );
			if (parent == null) {
				// happened once in a badly coded case
				parent = (Type) resolveOrClass(extractParentNameFromMethodFullname(name.toString()));
			}
		}
		else {
			parent = context.topType();
		}
		return parent;
	}

	protected Type getParentTypeFromNameOrContext(String name) {
		if (isFullyQualified(name)) {
			return resolveOrClass( extractParentNameFromMethodFullname(name) );
		}
		else {
			return context.topType();
		}
	}

}
