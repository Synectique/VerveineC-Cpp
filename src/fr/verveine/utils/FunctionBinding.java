package fr.verveine.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.text.html.parser.DTD;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.dom.ast.IFunctionType;
import org.eclipse.cdt.core.dom.ast.IParameter;
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
public class FunctionBinding implements IFunction {

	private static final String KEY_SEPARATOR = "/";

	/**
	 * The CDT binding of the entity
	 */
	protected IFunction cdtBnd;

	/**
	 * And the actual number of parameters
	 */
	protected int nbParam;

	/**
	 * A map of key/instances to make sure the same FunctionBinding instance is always associated to a IBinding+nbParam
	 */
	protected static Map<IFunction,Collection<FunctionBinding>> instances = new HashMap<IFunction, Collection<FunctionBinding>>();

	/**
	 * Returns a FunctionBinding instance that will serve as a key for a Famix Entity.<br>
	 * First, looks in an internal dictionary to see if there is already a FunctoinBinding for this CDT IBinding and this number of parameters
	 * if not found, creates, stores and returns a new FunctionBinding
	 */
	public static <T extends Entity>  IBinding getInstance(IBinding cdtBnd, int nbParam) {
		
		if ( ! (cdtBnd instanceof IFunction) ) {
			return cdtBnd;
		}

		Collection<FunctionBinding> lBindings = instances.get(cdtBnd);

		if (lBindings == null) {
			lBindings = new LinkedList<>();
		}
		
		for (FunctionBinding inst :  lBindings) {
			if (inst.getNbParam() == nbParam) {
				return inst;
			}
		}

		FunctionBinding inst = new FunctionBinding((IFunction) cdtBnd, nbParam);
		lBindings.add(inst);
		instances.put((IFunction) cdtBnd, lBindings);

		return inst;
	}

	private int getNbParam() {
		return nbParam;
	}

	private FunctionBinding(IFunction cdtBnd, int nbParam) {
		this.cdtBnd = cdtBnd;
		this.nbParam = nbParam;
	}

	/*
	 * IBinding API to implements.
	 * We do not actually use any of these 
	 */

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		return cdtBnd.getAdapter(arg0);
	}

	@Override
	public ILinkage getLinkage() {
		return cdtBnd.getLinkage();
	}

	@Override
	public String getName() {
		return cdtBnd.getName();
	}

	@Override
	public char[] getNameCharArray() {
		return cdtBnd.getNameCharArray();
	}

	@Override
	public IBinding getOwner() {
		return cdtBnd.getOwner();
	}

	@Override
	public IScope getScope() throws DOMException {
		return cdtBnd.getScope();
	}

	@Override
	public IScope getFunctionScope() {
		return cdtBnd.getFunctionScope();
	}

	@Override
	public IParameter[] getParameters() {
		return cdtBnd.getParameters();
	}

	@Override
	public IFunctionType getType() {
		return cdtBnd.getType();
	}

	@Override
	public boolean isAuto() {
		return cdtBnd.isAuto();
	}

	@Override
	public boolean isExtern() {
		return cdtBnd.isExtern();
	}

	@Override
	public boolean isInline() {
		return cdtBnd.isInline();
	}

	@Override
	public boolean isNoReturn() {
		return cdtBnd.isNoReturn();
	}

	@Override
	public boolean isRegister() {
		return cdtBnd.isRegister();
	}

	@Override
	public boolean isStatic() {
		return cdtBnd.isStatic();
	}

	@Override
	public boolean takesVarArgs() {
		return cdtBnd.takesVarArgs();
	}

}
