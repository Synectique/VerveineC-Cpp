package eu.synectique.verveine.extractor.cpp;

import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;

import ch.akuhn.fame.Repository;
import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;
import eu.synectique.verveine.core.gen.famix.Type;

public class CDictionary extends Dictionary<IBinding> {

	/**
	 * A property added to CompilationUnits to record the name of the source file they belong to.
	 * Used to create FileAnchors
	 */
	public static final String SOURCE_FILENAME_PROPERTY = "verveine-source-filename";

	/**
	 * @param famixRepo
	 */
	public CDictionary(Repository famixRepo) {
		super(famixRepo);
	}

	public Namespace createNamespace(String name, ScopingEntity parent) {
		Namespace fmx = super.createFamixEntity(Namespace.class, name, /*persistIt*/true);
		fmx.setParentScope(parent);

		return fmx;
	}

	public eu.synectique.verveine.core.gen.famix.Class createClass(String name, ContainerEntity parent) {
		eu.synectique.verveine.core.gen.famix.Class fmx;
		fmx = super.createFamixEntity(eu.synectique.verveine.core.gen.famix.Class.class, name, /*persistIt*/true);
		fmx.setContainer(parent);
		
		return fmx;
	}

	public Method createMethod(String name, Type parent) {
		Method fmx;
		fmx = super.createFamixEntity(Method.class, name, /*persistIt*/true);
		fmx.setParentType(parent);
		
		return fmx;
	}

	public Attribute createAttribute(String name, Type parent) {
		Attribute fmx;
		fmx = super.createFamixEntity(Attribute.class, name, /*persistIt*/true);
		fmx.setParentType(parent);
		
		return fmx;
	}

	
	
	
	public void addSourceAnchor(Class fmx, ICPPASTCompositeTypeSpecifier node, boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void ensureFamixParameter(IBinding iBinding, String name, Type paramType, Method fmxMth, boolean b) {
		// TODO Auto-generated method stub
		
	}

}
