package eu.synectique.verveine.extractor.plugin;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IBinding;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.BehaviouralReference;
import eu.synectique.verveine.core.gen.famix.CFile;
import eu.synectique.verveine.core.gen.famix.CompilationUnit;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.GlobalVariable;
import eu.synectique.verveine.core.gen.famix.Header;
import eu.synectique.verveine.core.gen.famix.ImplicitVariable;
import eu.synectique.verveine.core.gen.famix.Include;
import eu.synectique.verveine.core.gen.famix.IndexedFileAnchor;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Module;
import eu.synectique.verveine.core.gen.famix.MultipleFileAnchor;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.ParameterType;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ParameterizedType;
import eu.synectique.verveine.core.gen.famix.PreprocessorIfdef;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.SourceAnchor;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.core.gen.famix.UnknownVariable;
import eu.synectique.verveine.extractor.utils.FileUtil;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.utils.Visibility;
import eu.synectique.verveine.extractor.utils.WrongClassGuessException;

public class CDictionary extends Dictionary<IBinding> {

	/**
	 * Separator in fully qualified package name
	 */
	public static final String MOOSE_NAME_SEPARATOR = "::";

	/*
	 * names for primitive types
	 */
	private static final String PRIM_T_UNSPECIFIED = "unspecified";   // default to "int" but might also be for constructors (no return type)
	private static final String PRIM_T_BOOLEAN = "boolean";
	private static final String PRIM_T_INT = "int";
	private static final String PRIM_T_REAL = "real";
	private static final String PRIM_T_CHAR = "char";
	private static final String PRIM_T_UNKNOWN = "unknownPrimitiveType";

	protected Map<IBinding,CFile> nameToFile;

	public final static String DESTRUCTOR_KIND_MARKER = "destructor";
	
 	public CDictionary(Repository famixRepo) {
		super(famixRepo);
		nameToFile = new Hashtable<IBinding,CFile>();
	}

 	/**
 	 * for debugging purpose
 	 * @return number of entities in the repository (hence the dictionary)
 	 */
 	public int size() {
 		return famixRepo.size();
 	}

	@SuppressWarnings("unchecked")
 	public <T extends NamedEntity> T getEntityByKey(Class<T> clazz, IBinding key) {
 		NamedEntity found = super.getEntityByKey(key); 
		if ((found != null) && ! clazz.isInstance(found)) {
			WrongClassGuessException.reportWrongClassGuess(clazz, found);
			return null;
		}
		else {
			return (T) found;
		}
 	}

	@SuppressWarnings("unchecked")
	protected <T extends NamedEntity> T getEntityIfNotNull(Class<T> clazz, IBinding key) {
		if (key == null) {
			return null;
		}
		else {
			NamedEntity found = keyToEntity.get(key);
			if ((found != null) && ! clazz.isInstance(found)) {
				WrongClassGuessException.reportWrongClassGuess(clazz, found);
				return null;
			}
			else {
				return (T)found;
			}
		}
	}

	protected IndexedFileAnchor createIndexedSourceAnchor(String filename, int beg, int end) {
		IndexedFileAnchor fa;

		fa = new IndexedFileAnchor();
		fa.setStartPos(beg+1);
		fa.setEndPos(end+1);
		fa.setFileName( filename);

		famixRepoAdd(fa);

		return fa;
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

		if (anchor == null) {
			return null;
		}
		else {
			int beg = anchor.getNodeOffset();
			int end = beg + anchor.getNodeLength();

			return addSourceAnchor( fmx, filename, beg, end);
		}
	}

	public SourceAnchor addSourceAnchor(SourcedEntity fmx, String filename, int start, int end) {
			IndexedFileAnchor fa = null;

			if (fmx == null) {
				return null;
			}

			fa = createIndexedSourceAnchor(filename, start, end);
			fmx.setSourceAnchor(fa);

			return fa;
		}

	/**
	 * Adds location information to a Famix Entity that may be defined/declared in various files.
	 * Currently only used for BehaviouralEntities (functions, methods).
	 * Location informations are: <b>name</b> of the source file and <b>position</b> in this file.
	 * @param fmx -- Famix Entity to add the anchor to
	 * @param filename -- name of the file being visited
	 * @param ast -- ASTNode, where the information are extracted
	 * @return the Famix SourceAnchor added to fmx. May be null in case of incorrect/null parameter
	 */
	public SourceAnchor addSourceAnchorMulti(SourcedEntity fmx, String filename, IASTFileLocation anchor) {

		if (anchor == null) {
			return null;
		}
		else {
			int beg = anchor.getNodeOffset();
			int end = beg + anchor.getNodeLength();

			return addSourceAnchorMulti( fmx, filename, beg, end);
		}
	}

	public SourceAnchor addSourceAnchorMulti(SourcedEntity fmx, String filename, int start, int end) {
		MultipleFileAnchor mfa;

		if (fmx == null) {
			return null;
		}

		mfa = (MultipleFileAnchor) fmx.getSourceAnchor();
		if (mfa == null) {
			mfa = new MultipleFileAnchor();
			fmx.setSourceAnchor(mfa);
			famixRepoAdd(mfa);
		}

		// check if we already have this filename in the MultipleFileAnchor
		/*for (AbstractFileAnchor f : mfa.getAllFiles()) {
			if ( f.getFileName().equals(filename) ) {
				// note: Could check also the position in the file ...
				return mfa;
			}
		}*/

		mfa.addAllFiles( createIndexedSourceAnchor(filename, start, end) );

		return mfa;
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

	public BehaviouralReference addFamixBehaviouralPointer(BehaviouralEntity ref, BehaviouralEntity fmx) {
		BehaviouralReference pointer = new BehaviouralReference();
		pointer.setPointed(fmx);
		pointer.setReferer(ref);
		famixRepoAdd(pointer);
		return pointer;
	}

	public CFile ensureFamixCFile( IBinding key, String name) {
		CFile fmx = nameToFile.get(key);
		if (fmx == null) {
			if (FileUtil.isHeader(name)) {
				fmx = new Header();
			}
			else {
				fmx = new CompilationUnit();
			}
			fmx.setName(name);
			famixRepo.add(fmx);
			nameToFile.put(key, fmx);
		}
		
		return fmx;
	}

	public Module ensureFamixModule(IBinding key, String name, Package owner) {
		Module fmx = ensureFamixEntity(Module.class, key, name, /*persistIt*/true);
		fmx.setParentPackage(owner);

		return fmx;
	}

	public Include addFamixInclude(CFile src, CFile tgt) {
		if ( (src == null) || (tgt == null) ) {
			return null;
		}

		Include inc = new Include();
		inc.setTarget(tgt);
		inc.setSource(src);
		famixRepoAdd(inc);

		return inc;
	}

	public PreprocessorIfdef createFamixPreprocIfdef(String macroName) {
		PreprocessorIfdef fmx;

		fmx = new PreprocessorIfdef();
		fmx.setMacro(macroName);
		this.famixRepo.add(fmx);

		return fmx;
	}

	public UnknownVariable ensureFamixUnknownVariable(IBinding key, String name, Package parent) {
		UnknownVariable fmx = null;
		
		if (key != null) {
			fmx = getEntityByKey(UnknownVariable.class, key);
		}

		if (fmx == null) {
			fmx = super.ensureFamixEntity(UnknownVariable.class, key, name, /*persistIt*/true);
			fmx.setParentPackage(parent);
		}
		
		return fmx;
	}

	public GlobalVariable ensureFamixGlobalVariable(IBinding key, String name, ScopingEntity parent) {
		GlobalVariable fmx;
		fmx = ensureFamixEntity(GlobalVariable.class, key, name, /*persistIt*/true);
		fmx.setParentScope(parent);

		return fmx;
	}

	public <T extends NamedEntity> T ensureFamixEntity(Class<T> fmxClass, IBinding key, String name) {
		return super.ensureFamixEntity(fmxClass, key, name, /*persistIt*/true);
	}

	public Namespace ensureFamixNamespace(IBinding key, String name, ScopingEntity parent) {
		Namespace fmx = super.ensureFamixNamespace(key, name);
		if (parent != null) {
			fmx.setParentScope(parent);
		}
		return fmx;
	}

	public Package ensureFamixPackage(String name, Package parent) {
		IBinding key = StubBinding.getInstance(Package.class, mooseName(parent, name));
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
		Type fmx = getEntityIfNotNull(Type.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixType(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class ensureFamixClass(IBinding key, String name, ContainerEntity owner) {
		eu.synectique.verveine.core.gen.famix.Class fmx = getEntityIfNotNull(eu.synectique.verveine.core.gen.famix.Class.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterizableClass ensureFamixParameterizableClass(IBinding key, String name, ContainerEntity owner) {
		ParameterizableClass fmx = getEntityIfNotNull(ParameterizableClass.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixParameterizableClass(key, name, owner, /*persistIt*/true);
		}
		
		return fmx;
	}

	public ParameterType ensureFamixParameterType(IBinding key, String name, ContainerEntity owner) {
		ParameterType fmx = getEntityIfNotNull(ParameterType.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixParameterType(key, name, owner, /*persistIt*/true);
		}
		return fmx;
	}

	public ParameterizedType ensureFamixParameterizedType(IBinding key, String name, ParameterizableClass generic, ContainerEntity owner) {
		ParameterizedType fmx = getEntityIfNotNull(ParameterizedType.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixParameterizedType(key, name, generic, owner, /*persistIt*/true);
		}

		return fmx;
	}

	/** 
	 * Creating a "parameter type" depends on the context
	 * <ul>
	 * <li> If it is a ParameterizableClass (e.g. "<code>template &lt;class T&gt; class C</code> ...", we create a ParameterType
	 * <li> If it is a Method (e.g. "<code>template &lt;class T&gt; void fct(T)</code> ..."), we create a Type
	 * </ul>
	 */
	public eu.synectique.verveine.core.gen.famix.Type createParameterType(String name, ContainerEntity owner) {
		// apparently CDT gives a binding to the parameterType at its declaration ("template <class T> ...")
		// but not when used ("... mth(T)") so we ignore CDT binding and always use our custom build one
    	IBinding bnd;
    	bnd = StubBinding.getInstance(Type.class, mooseName(owner, name));

		if (owner instanceof ParameterizableClass) {
			return ensureFamixParameterType(bnd, name, owner);
		}
		else {
			return ensureFamixType(bnd, name, owner);
		}
	}

	/**
	 * May return null
	 */
	public Type ensureFamixPrimitiveType(int type) {
		StubBinding bnd = StubBinding.getInstance(Type.class, "_primitive_/"+type);
		return ensureFamixPrimitiveType(bnd, primitiveTypeName(type));
	}

	public Function ensureFamixFunction(IBinding key, String name, String sig, ContainerEntity parent) {
		Function fmx = getEntityIfNotNull(Function.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixFunction(key, name, sig, /*returnType*/null, parent, /*persistIt*/true);
			fmx.setCyclomaticComplexity(1);
			fmx.setNumberOfStatements(0);
		}
		return fmx;
	}

	public Method ensureFamixMethod(IBinding key, String name, String signature, Type parent) {
		Method fmx = getEntityIfNotNull(Method.class, key);

		if (fmx == null) {
			fmx = super.ensureFamixMethod(key, name, signature, /*returnType*/null, parent, /*persistIt*/true);
			fmx.setCyclomaticComplexity(1);
			fmx.setNumberOfStatements(0);
		}

		return fmx;
	}

	public Attribute ensureFamixAttribute(IBinding key, String name, Type parent) {
		Attribute fmx = getEntityIfNotNull(Attribute.class, key);

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
		fmx = getEntityByKey(Parameter.class, bnd);
		if (fmx != null) {
			return fmx;
		}

		if (fmx == null) {
			fmx = super.createFamixParameter(bnd, name, /*type*/null, owner, /*persistIt*/true);
		}

		return fmx;
	}

	public ImplicitVariable ensureFamixImplicitVariable(String name, Type type, BehaviouralEntity owner) {
		return super.ensureFamixImplicitVariable( name, type, owner, /*persistIt*/true);
	}

	/**
	 * Sets the visibility of a FamixNamedEntity.
	 * <code>null</code> visibility (e.g. in the case of a function) is silently ignored.
	 */
	public void setVisibility(NamedEntity fmx, Visibility visi) {
		if (visi != null) {
			fmx.addModifiers(visi.toString());
		}
	}

	// UTILITIES =========================================================================================================================================

	static public String primitiveTypeName(int type) {
		String name;
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
		case IASTSimpleDeclSpecifier.t_unspecified:
			name = PRIM_T_UNSPECIFIED;
			break;
		default:
			name = PRIM_T_UNKNOWN+"_"+type;
		}
		return name;
	}

	/**
	 * Computes moose name for an entity in a Container.
	 * This is a convenience method that delegates to one of {@link #mooseName(Function, String)}; {@link #mooseName(Method, String)}; {@link #mooseName(Namespace, String)};
	 * {@link #mooseName(Package, String)}; or {@link #mooseName(Type, String)}
	 * And this is required because at some point we need to call it with an unknown ContainerEntity :-(
	 */
	static public String mooseName(ContainerEntity parent, String name) {
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
		else { // e.g. parent == null
			return name;
		}
	}

	/**
	 * Computes moose name for a Namespace child.
	 * MooseName is the concatenation of the moosename of the parent Namescape with the simple name of the child
	 */
	static public String mooseName(Namespace parent, String name) {
		String ret;
		
		if (parent != null) {
			ret = concatMooseName( mooseName((Namespace)parent.getParentScope(), parent.getName()) , name);
		}
		else {
			ret = name;
		}

		return ret;
	}
	
	/**
	 * Computes moose name for a Package child
	 * MooseName is the concatenation of the moosename of the parent Package with the simple name of the child
	 */
	static public String mooseName(Package parent, String name) {
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
	static public String mooseName(Method parent, String name) {
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
	static public String mooseName(Function parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getContainer(), parent.getSignature()) , name);
		}
		else {
			return name;
		}
	}

	/**
	 * Computes moose name for a Type.
	 * MooseName is the concatenation of the mooseName of the parent package with the simple name of the type
	 */
	static public String mooseName(Type parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getContainer(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}

	static protected String concatMooseName(String prefix, String name) {
		return prefix + MOOSE_NAME_SEPARATOR + name;
	}

	/**
	 * Remove a ParameterEntity from the repository
	 * Parameter is also removed from its parentBehaviouralEntity
	 */
	public void removeParameter(Parameter param) {
		param.setParentBehaviouralEntity(null);
		super.removeEntity(param);
	}

}
