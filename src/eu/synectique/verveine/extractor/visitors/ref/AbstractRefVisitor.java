package eu.synectique.verveine.extractor.visitors.ref;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateInstance;
import org.eclipse.cdt.core.dom.ast.gnu.c.ICASTKnRFunctionDeclarator;
import org.eclipse.cdt.core.index.IIndex;

import eu.synectique.verveine.core.EntityStack;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.BehaviouralReference;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.ContainerEntity;
import eu.synectique.verveine.core.gen.famix.DereferencedInvocation;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ParameterizedType;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.QualifiedName;
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


	public AbstractRefVisitor(CDictionary dico, IIndex index, String rootFolder) {
		super(dico, index, rootFolder);
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

		this.getContext().push(fmx);
		for (IASTDeclaration decl : node.getDeclarations(/*includeInactive*/true)) {
			decl.accept(this);
		}
		returnedEntity = getContext().pop();

		return PROCESS_SKIP;
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
	protected int visit(IASTNamedTypeSpecifier node) {
		returnedEntity = referedType(node.getName());
		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		// get node name and bnd
		super.visit(node);
		returnedEntity = resolver.ensureBehavioural(node, nodeBnd, nodeName);

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(IASTFunctionDefinition node) {
		returnedEntity = null;

		node.getDeclarator().accept(this);

		this.getContext().push((BehaviouralEntity)returnedEntity);

		node.getBody().accept(this);

		returnedEntity = this.getContext().pop();

		return PROCESS_SKIP;
	}

	/*
	 * need to have ICPPASTFunctionDefinition also because it has specificities not found in IASTFunctionDefinition
	 * (MemberInitializers)
	 */
	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		returnedEntity = null;

		visit( (IASTFunctionDefinition)node);  // visit declarator and body (see above)

		this.getContext().push((BehaviouralEntity)returnedEntity);

		for (ICPPASTConstructorChainInitializer init : node.getMemberInitializers()) {
			init.accept(this);
		}

		returnedEntity = this.getContext().pop();

		return PROCESS_SKIP;
	}




	/**
	 * Find a referenced type from its name
	 * May have to create it if it is not found
	 */
	protected Type referedType(IASTName name) {
		Type fmx = null;
		IBinding bnd = resolver.getBinding( name);

		if (bnd == null) {
			bnd = resolver.mkStubKey(name, Type.class);
		}

		NamedEntity tmp = dico.getEntityByKey(bnd);
		fmx = (Type) tmp;

		if (fmx == null) {	// try to find it in the current context despite the fact that we don't have a IBinding
			fmx = (Type) resolver.findInParent(name.toString(), getContext().top(), /*recursive*/true);
		}

		if (fmx == null) {  // still not found, create it
			if (isParameterTypeInstanceName(name.toString())) {
				fmx = referedParameterTypeInstance(bnd, name);
			}
			else {
				QualifiedName qualName = new QualifiedName(name);
				fmx = dico.ensureFamixType(bnd, qualName.unqualifiedName(), /*owner*/(ContainerEntity)resolver.resolveOrNamespace(qualName.nameQualifiers().toString()));
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
		String typName = new QualifiedName(strName.substring(0, i)).unqualifiedName();

		ParameterizedType fmx = null;
		ParameterizableClass generic = null;
		try {
			generic = (ParameterizableClass) resolver.findInParent(typName, getContext().top(), /*recursive*/true);
		}
		catch (ClassCastException e) {
			// create a ParameterizedType for an unknown generic
			// 'generic' var. remains null
		}
		fmx = dico.ensureFamixParameterizedType(bnd, typName, generic, (ContainerEntity)resolver.resolveOrNamespace(new QualifiedName(name).nameQualifiers().toString()));

		for (String typArg : strName.substring(i+1, strName.length()-1).split(",")) {
			typArg = typArg.trim();
			try {
				Type arg = (Type) resolver.findInParent(typArg, getContext().top(), /*recursive*/true);
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
		BehaviouralEntity accessor = this.getContext().topBehaviouralEntity();
		Invocation invok = dico.addFamixInvocation(accessor, fmx, /*receiver*/null, /*signature*/null, getContext().getLastInvocation());
		getContext().setLastInvocation(invok);
		return invok;
	}

	/**
	 * Records an Invocation of a BehaviouralEntity referenced by a variable (a pointer) and sets lastInvocation attribute.
	 * Assumes the context is correctly set (i.e. top contains a BehaviouralEntity that makes the invocation) 
	 * @param fmx -- StructuralEntity pointing to a BehaviouralEntity invoked
	 * @return the invocation created
	 */
	protected DereferencedInvocation dereferencedInvocation(StructuralEntity fmx, String sig) {
		BehaviouralEntity accessor = this.getContext().topBehaviouralEntity();
		DereferencedInvocation invok = dico.addFamixDereferencedInvocation(accessor, fmx, /*signature*/sig, getContext().getLastInvocation());
		getContext().setLastInvocation(invok);
		return invok;
	}

	/**
	 * Records a reference (pointer) to a famixBehaviouralEntity.
	 * Assumes the context is correctly set (i.e. top contains another BehaviouralEntity that makes the reference) 
	 * @param fmx -- referenced BehaviouralEntity
	 * @return the reference created
	 */
	protected BehaviouralReference behaviouralPointer(BehaviouralEntity fmx) {
		BehaviouralEntity referer = this.getContext().topBehaviouralEntity();
		BehaviouralReference ref = dico.addFamixBehaviouralPointer(referer, fmx);
		return ref;
	}


}