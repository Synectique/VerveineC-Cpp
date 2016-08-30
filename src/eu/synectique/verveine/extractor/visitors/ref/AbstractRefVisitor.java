package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateInstance;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.internal.core.pdom.dom.cpp.IPDOMCPPClassType;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.BehaviouralReference;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ParameterizedType;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;

/**
 * Abstract superclass for Reference visitors.<BR>
 * It defines some utility methods to create references to names.
 * @author anquetil
 */
@SuppressWarnings("unused")
public abstract class AbstractRefVisitor extends AbstractVisitor {

	/**
	 * see {@link #returnedEntity}
	 */
	public SourcedEntity returnedEntity() {
		return returnedEntity;
	}


	public AbstractRefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);
	}


	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
		returnedEntity = referedType(node.getName());
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTElaboratedTypeSpecifier node) {
		returnedEntity = referedType(node.getName());
		return PROCESS_SKIP;
	}

	@Override
	public int visit(IASTSimpleDeclSpecifier node) {
		returnedEntity = dico.ensureFamixPrimitiveType( ((IASTSimpleDeclSpecifier) node).getType());
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTEnumerationSpecifier node) {
		returnedEntity = referedType(node.getName());
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
		returnedEntity = referedType(node.getName());
		return PROCESS_SKIP;
	}

	/*
	 * putting class definition on the context stack
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		Class fmx;

		/* Gets the key (IBinding) of the node to recover the famix type entity */
		super.visit(node);

		fmx = (Class) dico.getEntityByKey(nodeBnd);

		this.context.push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = context.pop();

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		 returnedEntity = null;

		// compute nodeName and binding
		super.visit(node);

		// just in case this is a definition and we already processed the declaration
		returnedEntity = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);
		// try harder
		if (returnedEntity == null) {
			returnedEntity = resolveBehaviouralFromName(node, nodeBnd);
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		returnedEntity = null;

		((ICPPASTFunctionDeclarator)node.getDeclarator()).accept(this);

		this.context.push((BehaviouralEntity)returnedEntity);

		for (ICPPASTConstructorChainInitializer init : node.getMemberInitializers()) {
			init.accept(this);
		}

		node.getBody().accept(this);

		this.context.pop();

		return PROCESS_SKIP;
	}




	/**
	 * Find a referenced type from its name
	 * May have to create it if it is not found
	 */
	protected Type referedType(IASTName name) {
		Type fmx = null;
		IBinding bnd = getBinding( name);

		if (bnd == null) {
			bnd = StubBinding.getInstance(Type.class, dico.mooseName(context.getTopCppNamespace(), name.toString()));
		}

		fmx = (Type) dico.getEntityByKey(bnd);

		if (fmx == null) {	// try to find it in the current context despite the fact that we don't have a IBinding
			fmx = (Type) findInParent(name.toString(), context.top(), /*recursive*/true);
		}

		if (fmx == null) {  // still not found, create it
			if (isParameterTypeInstanceName(name.toString())) {
				fmx = referedParameterTypeInstance(bnd, name);
			}
			else {
				fmx = dico.ensureFamixType(bnd, simpleName(name), /*owner*/getParentOfFullyQualifiedName(name));
			}
		}

		return fmx;
	}

	/**
	 * Creates a ParameterizedType, if possible in link with its ParameterizableClass
	 * Puts parameterTypes argument into the ParameterizedType when possible
	 */
	private Type referedParameterTypeInstance(IBinding bnd, IASTName name) {
		String strName = name.toString();
		int i = strName.indexOf('<');
		String typName = simpleName(strName.substring(0, i));

		ParameterizedType fmx = null;
		ParameterizableClass generic = null;
		try {
			generic = (ParameterizableClass) findInParent(typName, context.top(), /*recursive*/true);
		}
		catch (ClassCastException e) {
			// create a ParameterizedType for an unknown generic
			// 'generic' var. remains null
		}
		fmx = dico.ensureFamixParameterizedType(bnd, typName, generic, getParentOfFullyQualifiedName(name));

		for (String typArg : strName.substring(i+1, strName.length()-1).split(",")) {
			typArg = typArg.trim();
			try {
				Type arg = (Type) findInParent(typArg, context.top(), /*recursive*/true);
				if (arg != null) {
					fmx.addArguments(arg);
				}
			}
			catch (ClassCastException e) {
				// for some reason, findInParent seems to have found an entity with this name but not a Type
				// just forget about it
			}
		}
		
		return fmx;
	}

	private boolean isParameterTypeInstanceName(String name) {
		return (name.indexOf('<') > 0) && (name.endsWith(">"));
	}

	/**
	 * Records an Invocation of a famixBehaviouralEntity and sets lastInvocation attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the invocation) 
	 * @param fmx -- invoked BehaviouralEntity
	 * @return the invocation created
	 */
	protected Invocation invocationOfBehavioural(BehaviouralEntity fmx) {
		BehaviouralEntity accessor = this.context.topBehaviouralEntity();
		Invocation invok = dico.addFamixInvocation(accessor, fmx, /*receiver*/null, /*signature*/null, context.getLastInvocation());
		context.setLastInvocation(invok);
		return invok;
	}

	/**
	 * Records an Invocation of a BehaviouralEntity referenced by a variable (a pointer) and sets lastInvocation attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the invocation) 
	 * @param fmx -- StructuralEntity pointing to a BehaviouralEntity invoked
	 * @return the invocation created
	 */
	protected DereferencedInvocation dereferencedInvocation(StructuralEntity fmx, String sig) {
		BehaviouralEntity accessor = this.context.topBehaviouralEntity();
		DereferencedInvocation invok = dico.addFamixDereferencedInvocation(accessor, fmx, /*signature*/sig, context.getLastInvocation());
		context.setLastInvocation(invok);
		return invok;
	}

	/**
	 * Records a reference (pointer) to a famixBehaviouralEntity.
	 * Assumes the context is correctly set (i.e. top contains another BehaviouralEntity that makes the reference) 
	 * @param fmx -- referenced BehaviouralEntity
	 * @return the reference created
	 */
	protected BehaviouralReference behaviouralPointer(BehaviouralEntity fmx) {
		BehaviouralEntity referer = this.context.topBehaviouralEntity();
		BehaviouralReference ref = dico.addFamixBehaviouralPointer(referer, fmx);
		return ref;
	}

}