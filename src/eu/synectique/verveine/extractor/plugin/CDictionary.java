package eu.synectique.verveine.extractor.plugin;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IBinding;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.BehaviouralReference;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.IndexedFileAnchor;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ParameterizedType;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.SourceAnchor;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.core.gen.famix.UnknownVariable;
import eu.synectique.verveine.extractor.utils.StubBinding;

public class CDictionary extends Dictionary<IBinding> {

	/**
	 * Separator in fully qualified package name
	 */
	public static final String CPP_NAME_SEPARATOR = "::";

	/**
	 * Separator in fully qualified package name
	 */
	public static final String MOOSE_NAME_SEPARATOR = "::";

	public final static String DESTRUCTOR_KIND_MARKER = "destructor";

	/*
	 * names for primitive types
	 */
	private static final String PRIM_T_BOOLEAN = "boolean";
	private static final String PRIM_T_INT = "int";
	private static final String PRIM_T_REAL = "real";
	private static final String PRIM_T_CHAR = "char";
	private static final String PRIM_T_UNKNOWN = "unknownPrimitiveType";

 	public CDictionary(Repository famixRepo) {
		super(famixRepo);
	}

	protected NamedEntity getEntityIfNotNull(IBinding key) {
		if (key == null) {
			return null;
		}
		else {
			return keyToEntity.get(key);
		}
	}
	
	/**
	 * Adds location information to a Famix Entity.
	 * Location informations are: <b>name</b> of the source file and <b>position</b> in this file.
	 * @param fmx -- Famix Entity to add the anchor to
	 * @param filename -- name of the file being visited
	 * @param ast -- ASTNode, where the information are extracted
	 * @return the Famix SourceAnchor added to fmx. May be null in case of incorrect/null parameter
	 */
	public SourceAnchor addSourceAnchor(SourcedEntity fmx, String filename, IASTFileLocation anchor) {
		IndexedFileAnchor fa = null;

		if ( (fmx == null) || (anchor == null) ) {
			return null;
		}

		// position in source file
		int beg = anchor.getNodeOffset();
		int end = beg + anchor.getNodeLength();

		// create the Famix SourceAnchor
		fa = new IndexedFileAnchor();
		fa.setStartPos(beg);
		fa.setEndPos(end);
		fa.setFileName(filename);

		fmx.setSourceAnchor(fa);
		famixRepo.add(fa);

		return fa;
	}

	public DereferencedInvocation addFamixDereferencedInvocation(BehaviouralEntity sender, StructuralEntity referencer, String signature, Association prev) {
		DereferencedInvocation invok = new DereferencedInvocation();
		invok.setSender(sender);
		invok.setReferencer(referencer);
		chainPrevNext(prev, invok);
		famixRepoAdd(invok);

		if (signature != null) {
			invok.setSignature(signature);
		}

		return invok;
	}

	public BehaviouralReference addFamixBehaviouralReference(BehaviouralEntity ref, BehaviouralEntity fmx) {
		BehaviouralReference pointer = new BehaviouralReference();
		pointer.setPointed(fmx);
		pointer.setReferer(ref);
		famixRepoAdd(pointer);
		return pointer;
	}

	/**
//	 * Create an UnknownVariable. parent currently not used
	 */
	public UnknownVariable createFamixUnknownVariable(String name, NamedEntity parent) {
		UnknownVariable fmx;
		
		fmx = ensureFamixEntity(UnknownVariable.class, /*key*/null, name, /*persistIt*/true);
		fmx.setIsStub(true);
		
		return fmx;
	}

	public Namespace ensureFamixNamespace(IBinding key, String name, ScopingEntity parent) {
		Namespace fmx = super.ensureFamixNamespace(key, name);
		if (parent != null) {
			fmx.setParentScope(parent);
		}
		return fmx;
	}

	public Package ensureFamixPackage(String name, Package parent) {
		String fullname = mooseName(parent, name);
		IBinding key = StubBinding.getInstance(Package.class, fullname);
		Package fmx = super.ensureFamixEntity(Package.class, key, name, /*persitIt*/true);
		fmx.setIsStub(false);
		if (parent != null) {
			fmx.setParentPackage(parent);
		}
		return fmx;
	}

	public TypeAlias ensureFamixTypeAlias(IBinding key, String name, ContainerEntity owner) {
		TypeAlias fmx;

		fmx = super.ensureFamixEntity(TypeAlias.class, key, name, /*persistIt*/true);
		fmx.setContainer(owner);

		return fmx;
	}

	public Type ensureFamixType(IBinding key, String name, ContainerEntity owner) {
		Type fmx;
		fmx = (Type) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixType(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class ensureFamixClass(IBinding key, String name, ContainerEntity owner) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = (Class) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterizableClass ensureFamixParameterizableClass(IBinding key, String name, ContainerEntity owner) {
		ParameterizableClass fmx;
		fmx = (ParameterizableClass) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixParameterizableClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterizedType ensureFamixParameterizedType(IBinding key, String name, ParameterizableClass generic, ContainerEntity owner) {
		ParameterizedType fmx;
		fmx = (ParameterizedType) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixParameterizedType(key, name, generic, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public Type ensureFamixPrimitiveType(int type) {
		StubBinding bnd = StubBinding.getInstance(Type.class, "_primitive_/"+type);
		 return ensureFamixPrimitiveType(bnd, primitiveTypeName(type));
	}

	public Function ensureFamixFunction(IBinding key, String name, String sig, ContainerEntity parent) {
		Function fmx;
		fmx = (Function) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixFunction(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);
			fmx.setCyclomaticComplexity(1);
			fmx.setNumberOfStatements(0);
		}
		return fmx;
	}

	public Method ensureFamixMethod(IBinding key, String name, String signature, Type parent) {
		Method fmx;
		fmx = (Method) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixMethod(key, name, signature, /*returnType*/null, parent, /*persistIt*/true);
			fmx.setCyclomaticComplexity(1);
			fmx.setNumberOfStatements(0);
		}

		return fmx;
	}

	public Attribute ensureFamixAttribute(IBinding key, String name, Type parent) {
		Attribute fmx;
		fmx = (Attribute) getEntityIfNotNull(key);
		if (fmx == null) {
			fmx = super.ensureFamixAttribute(key, name, /*type*/null, parent, /*persistIt*/true);
		}

		return fmx;
	}

	/**
	 * Returns a Famix Parameter associated with the IBinding.
	 * The Entity is created if it does not exist.<br>
	 * Params: see {@link Dictionary#ensureFamixParameter(Object, String, Type, eu.synectique.verveine.core.gen.famix.BehaviouralEntity, boolean)}.
	 * @param persistIt -- whether to persist or not the entity eventually created
	 * @return the Famix Entity found or created. May return null if "bnd" is null or in case of a Famix error
	 */
	public Parameter ensureFamixParameter(IBinding bnd, String name, BehaviouralEntity owner) {
		Parameter fmx = null;

		// --------------- to avoid useless computations if we can
		fmx = (Parameter)getEntityByKey(bnd);
		if (fmx != null) {
			return fmx;
		}

		if (fmx == null) {
			fmx = super.createFamixParameter(bnd, name, /*type*/null, owner, /*persistIt*/true);
		}

		return fmx;
	}

	// UTILITIES =========================================================================================================================================

	public String primitiveTypeName(int type) {
		String name = PRIM_T_UNKNOWN+"_"+type;
		switch (type) {
		case IASTSimpleDeclSpecifier.t_void:
			// for type void, we return null as in: "void f()"
			// but this might not be a good idea, as in: "void *p"
			return null;
		case IASTSimpleDeclSpecifier.t_bool:
			name = PRIM_T_BOOLEAN;
			break;
		case IASTSimpleDeclSpecifier.t_char:
		case IASTSimpleDeclSpecifier.t_char16_t:
		case IASTSimpleDeclSpecifier.t_char32_t:
		case IASTSimpleDeclSpecifier.t_wchar_t:
			name = PRIM_T_CHAR;
			break;
		case IASTSimpleDeclSpecifier.t_decimal32:
		case IASTSimpleDeclSpecifier.t_decimal64:
		case IASTSimpleDeclSpecifier.t_decimal128:
		case IASTSimpleDeclSpecifier.t_int:
		case IASTSimpleDeclSpecifier.t_int128:
			name = PRIM_T_INT;
			break;
		case IASTSimpleDeclSpecifier.t_float:
		case IASTSimpleDeclSpecifier.t_float128:
		case IASTSimpleDeclSpecifier.t_double:
			name = PRIM_T_REAL;
			break;
		}
		return name;
	}

	/**
	 * Computes moose name for an entity in a Container.
	 * This is a convenience method that delegates to one of {@link #mooseName(Function, String)}; {@link #mooseName(Method, String)}; {@link #mooseName(Namespace, String)};
	 * {@link #mooseName(Package, String)}; or {@link #mooseName(Type, String)}
	 * And this is required because at some point we need to call it with an unknown ContainerEntity :-(
	 */
	public String mooseName(ContainerEntity parent, String name) {
		if (parent instanceof Namespace) {
			return mooseName((Namespace)parent, name);
		}
		else if (parent instanceof Package) {
			return mooseName((Package)parent, name);
		}
		else if (parent instanceof Type) {
			return mooseName((Type)parent, name);
		}
		else if (parent instanceof Method) {
			return mooseName((Method)parent, name);
		}
		else if (parent instanceof Function) {
			return mooseName((Function)parent, name);
		}
		else {
			return name;
		}
	}

	/**
	 * Computes moose name for a Namespace child.
	 * MooseName is the concatenation of the moosename of the parent Namescape with the simple name of the child
	 */
	public String mooseName(Namespace parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName((Namespace)parent.getParentScope(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}
	
	/**
	 * Computes moose name for a Package child
	 * MooseName is the concatenation of the moosename of the parent Package with the simple name of the child
	 */
	public String mooseName(Package parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentPackage(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}

	/**
	 * Computes moose name for a Method child.
	 * MooseName is the concatenation of the moosename of the parent Mathod with the simple name of the child
	 */
	public String mooseName(Method parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentType(), parent.getSignature()) , name);
		}
		else {
			return name;
		}
	}

	/**
	 * Computes moose name for a Function child.
	 * MooseName is the concatenation of the moosename of the parent Function with the simple name of the child
	 */
	public String mooseName(Function parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getContainer(), parent.getSignature()) , name);
		}
		else {
			return name;
		}
	}

	/**
	 * Computes moose name for a Type.
	 * MooseName is the concatenation of the moosename of the parent package with the simple name of the type
	 */
	public String mooseName(Type parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getContainer(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}

	protected static String concatMooseName(String prefix, String name) {
		return prefix + MOOSE_NAME_SEPARATOR + name;
	}

}