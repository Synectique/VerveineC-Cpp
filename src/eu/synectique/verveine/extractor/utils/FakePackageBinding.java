package eu.synectique.verveine.extractor.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.core.runtime.CoreException;

import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.ScopingEntity;

/**
 * This is a CDT {@link IIndexBinding} implementor to serve as key for FamixPackages
 * Rational: The Famix dictionary needs as entity key an IIndexBinding.
 * But the Packages or file directories, they don't have associated CDT binding
 * So we create this class that will implement a fake IIndexBinding for each FamixPackage
 * The actual key will be the fully qualified name of the Package
 * @author anquetil
 */
public class FakePackageBinding implements IIndexBinding {

	public static final String NAME_SEPARATOR = "::";
	/**
	 * The actual key of a package
	 */
	protected String fullname;

	/**
	 * A map of key/instances to make sure the same instance is always associated to a given key
	 */
	protected static Map<String,FakePackageBinding> instances = new HashMap<String, FakePackageBinding>();

	/**
	 * Returns an instance from a Package name and owner.<br>
	 * First, computes the full name of the package, then looks in an internal dictionary to see if there is already
	 * an instance for that fullname, if not creates such an instance, returns the instance
	 * @param name of the Package
	 * @param parent of the PAckae (another Package, possibly <code>null</code>)
	 * @return the FakePackageBinding associated with the fullname of the package
	 */
	public static FakePackageBinding getInstance(String name, eu.synectique.verveine.core.gen.famix.Package parent) {
		String instName = mooseName(parent, name);
		FakePackageBinding inst;
		
		inst = instances.get(instName);
		if (inst == null) {
			inst = new FakePackageBinding(instName);
			instances.put(instName, inst);
		}
		return inst;
	}

	private FakePackageBinding(String fullname) {
		this.fullname = fullname;
	}

	/**
	 * Computes moose name for a ScopingEntity
	 * This is a convenient method to call {@link #mooseName(Namespace)} or {@link #mooseName(Package)}
	 * and to make Java type checker happy
	 */
	protected static String mooseName(ScopingEntity ent, String name) {
		if (ent instanceof Package) {
			return mooseName((Package)ent, name);
		}
		if (ent instanceof Namespace) {
			return mooseName((Namespace)ent, name);
		}
		return name;
	}

	/**
	 * Computes moose name for a Namespace. NOT USED CURRENTLY (but this may change)
	 * MooseName is the concatenation of the moosename of the parent Namescape with the simple name of the Namescape
	 */
	protected static String mooseName(Namespace parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentScope(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}
	
	/**
	 * Computes moose name for a Package
	 * MooseName is the concatenation of the moosename of the parent Package with the simple name of the Package
	 */
	protected static String mooseName(Package parent, String name) {
		if (parent != null) {
			return concatMooseName( mooseName(parent.getParentPackage(), parent.getName()) , name);
		}
		else {
			return name;
		}
	}

	protected static String concatMooseName(String prefix, String name) {
		return prefix + NAME_SEPARATOR + name;
	}

	/*
	 * IIndexBinding API to implements.
	 * We do not actually use any of these 
	 */
	
	@Override
	public ILinkage getLinkage() {
		return null;
	}

	@Override
	public String getName() {
		// we might as well return fullname since we have it
		return fullname;
	}

	@Override
	public char[] getNameCharArray() {
		// we might as well return fullname since we have it
		return fullname.toCharArray();
	}

	@Override
	public IScope getScope() throws DOMException {
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		return null;
	}

	@Override
	public IIndexFile getLocalToFile() throws CoreException {
		return null;
	}

	@Override
	public IIndexBinding getOwner() {
		int i = fullname.lastIndexOf(NAME_SEPARATOR);
		if (i < 0) {
			return null;
		}
		else {
			return instances.get(fullname.substring(0, i-1));
		}
	}

	@Override
	public String[] getQualifiedName() {
		// we might as well return fullname since we have it
		return fullname.split(NAME_SEPARATOR);
	}

	@Override
	public boolean isFileLocal() throws CoreException {
		return false;
	}

}
