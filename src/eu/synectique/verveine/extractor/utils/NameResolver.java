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
		QualifiedName qualName = new QualifiedName(name);

		if (qualName.isFullyQualified()) {
			parent = (ContainerEntity) resolveOrNamespace(qualName.nameQualifiers().toString());
			simpleName = qualName.unqualifiedName();
		}
		else {
			simpleName = name;
		}
		return StubBinding.getInstance(entityType, dico.mooseName(parent, simpleName));
	}

	/**
	 * Forges a signature for stub BehaviouralEntities
	 * @return "name(&lt;parameters&gt;)" where parametres are substitued by "_" 
	 */
	public String mkStubSig(String name, int nbParam) {
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

			String[] parts = fullName.split(QualifiedName.CPP_NAME_SEPARATOR);
			
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
			return new QualifiedName(((StubBinding)bnd).getEntityName()).unqualifiedName().charAt(0) == '~';
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
			QualifiedName qualName = new QualifiedName(name);
			if (qualName.isFullyQualified()) {
				parent = (ContainerEntity) resolveOrNamespace(qualName.nameQualifiers().toString());
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
				fmx = dico.ensureFamixMethod(bnd, new QualifiedName(name).unqualifiedName(), mthSig, /*owner*/parent);
			}
		}
		else {                    //   C function or may be a stub ?
			fmx = dico.ensureFamixFunction(bnd, new QualifiedName(name).unqualifiedName(), computeSignatureFromAST(node), (ContainerEntity)context.top());
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
		return resolveOrNamespace(new QualifiedName(name));
	}

	public NamedEntity resolveOrNamespace( QualifiedName name) {
		NamedEntity tmp;
		String simpleName;
		ContainerEntity parent = null;
		boolean recursive;

		if (name.isEmpty()) {
			return null;
		}

		simpleName = name.unqualifiedName();

		if (name.isFullyQualified()) {
			// parent is described by nameQualifiers, no recursive search in it
			parent = (ContainerEntity)resolveOrNamespace(name.nameQualifiers());
			recursive = false;
		}
		else {			
			if (name.isAbsoluteQualified()) {
				// parent is root, no recursive search
				parent = null;
				recursive = false;
			}
			else {
				// parent is current stack context, recursive search in it
				parent = (ContainerEntity) context.top();
				recursive = true;
			}
		}

		tmp = findInParent(simpleName, parent, recursive);

		if (tmp == null) {
			IBinding bnd;

			if (recursive || (parent == null) ) {
				// if recursive true, we were looking for an unqualified name and did not find it, so we create a namespace at toplevel
				// if parent == null, we are at top level so it is similar
				bnd = mkStubKey(simpleName, /*parent*/null, Namespace.class);
				tmp = dico.ensureFamixNamespace(bnd, simpleName, /*owner*/null);
			}
			else if (parent instanceof Namespace) {
				// default case
				bnd = mkStubKey(simpleName, parent, Namespace.class);
				tmp = dico.ensureFamixNamespace(bnd, simpleName, (ScopingEntity) parent);
			}
			else {
				// otherwise, if parent is not a namespace (probably a class or method), then we create a Class because we can't have a namespace inside a class or method
				bnd = mkStubKey(simpleName, parent, eu.synectique.verveine.core.gen.famix.Class.class);
				tmp = dico.ensureFamixClass(bnd, simpleName, parent);
			}
		}

		return tmp;
	}

	/**
	 * Ensures a (stub) type/class and its parent (a namespace).
	 * Deals with fully qualified class name and not qualified class name
	 */
	public Type resolveOrClass(String name) {
		return resolveOrClass(new QualifiedName(name));
	}

	public Type resolveOrClass(QualifiedName name) {
		Type tmp;
		String simpleName;
		ContainerEntity parent = null;
		boolean recursive;

		if (name.isEmpty()) {
			return null;
		}

		simpleName = name.unqualifiedName();

		if (name.isFullyQualified()) {
			// parent is described by nameQualifiers, no recursive search in it
			parent = (ContainerEntity)resolveOrNamespace(name.nameQualifiers());
			recursive = false;
		}
		else {			
			if (name.isAbsoluteQualified()) {
				// parent is root, no recursive search
				parent = null;
				recursive = false;
			}
			else {
				// parent is current stack context, recursive search in it
				parent = (ContainerEntity) context.top();
				recursive = true;
			}
		}

		tmp = (Type) findInParent(simpleName, parent, recursive);

		if (tmp == null) {
			IBinding bnd;

			bnd = mkStubKey(simpleName, parent, eu.synectique.verveine.core.gen.famix.Class.class);
			tmp = dico.ensureFamixClass(bnd, simpleName, parent);
		}

		return tmp;
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
		if (QualifiedName.isFullyQualified(fullname)) {
			int i;
			i = fullname.indexOf('(');
			i = fullname.substring(0, i).lastIndexOf(QualifiedName.CPP_NAME_SEPARATOR);
			return fullname.substring(i+QualifiedName.CPP_NAME_SEPARATOR.length());
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

		i = fullname.lastIndexOf(QualifiedName.CPP_NAME_SEPARATOR);
		return fullname.substring(0, i);
	}

	protected Type getMethodParentFromBindingOrContext(IBinding bnd, IASTName name) {
		Type parent;
		if (QualifiedName.isFullyQualified(name)) {
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
		if (QualifiedName.isFullyQualified(name)) {
			return resolveOrClass( extractParentNameFromMethodFullname(name) );
		}
		else {
			return context.topType();
		}
	}

}
