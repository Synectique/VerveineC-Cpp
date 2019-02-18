package fr.verveine.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;

import eu.synectique.verveine.core.gen.famix.Entity;
import eu.synectique.verveine.core.gen.famix.Package;

/**
 * This is a CDT {@link IBinding} implementor to serve as key for unresolved entities
 * Rational: The Famix dictionary needs an IBinding as entity key.
 * But the stubs (and FamixPackages) don't have associated CDT binding
 * So we create this class that will implement a fake IBinding for each stub
 * The actual key will be some string including its Famix type (e.g. "Package") and a name (e.g. the fully qualified name of a Package)
 * (see also {@link #getInstance(String, Package)})
 * @author Anquetil and Bhatti
 */
public class StubBinding implements IBinding {

	private static final String KEY_SEPARATOR = "/";

	/**
	 * The actual key of the entity
	 */
	protected String keyname;

	/**
	 * A map of key/instances to make sure the same StubBinding instance is always associated to a given key
	 */
	protected static Map<String,StubBinding> instances = new HashMap<String, StubBinding>();

	/**
	 * Returns a StubBinding instance that will serve as a key for a Famix Entity.<br>
	 * First, computes the keyname of the entity (Class name + entity name), then looks in an internal dictionary to see if there is already
	 * a StubBinding for that keyname, if not creates such StubBinding, returns the StubBinding
	 * <p>
	 * Note: the clazz parameter should really be a subtype of NamedEntity. However, we need it to also accept {@link CFile}, so had to accept all sub-types of Entity :-( 
	 * @param clazz -- Famix class of the entity for which we need a key
	 * @param id -- some string identifying as uniquely as possible the entity, within its famix class (e.g. fully qualified package name, name and number of parameter of a method, etc.)
	 * @return the StubBinding associated with the entity
	 */
	public static <T extends Entity>  StubBinding getInstance(Class<T> clazz, String id) {
		String key = clazz.getName() + KEY_SEPARATOR + id;
		StubBinding inst;
		
		inst = instances.get(key);
		if (inst == null) {
			inst = new StubBinding(key);
			instances.put(key, inst);
		}
		return inst;
	}

	private StubBinding(String keyname) {
		this.keyname = keyname;
	}

	public String getEntityClass() {
		int i;
		i = keyname.indexOf(KEY_SEPARATOR);
		return keyname.substring(0, i);
	}


	public String getEntityName() {
		int i;
		i = keyname.indexOf(KEY_SEPARATOR);
		return keyname.substring(i+1);
	}

	/*
	 * IBinding API to implements.
	 * We do not actually use any of these 
	 */

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		return null;
	}

	@Override
	public ILinkage getLinkage() {
		return null;
	}

	@Override
	public String getName() {
		return getEntityName();
	}

	@Override
	public char[] getNameCharArray() {
		return getName().toCharArray();
	}

	@Override
	public IBinding getOwner() {
		return null;
	}

	@Override
	public IScope getScope() throws DOMException {
		return null;
	}

}
