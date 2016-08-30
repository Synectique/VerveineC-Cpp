package eu.synectique.verveine.extractor.visitors.ref;

import java.io.RandomAccessFile;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNameOwner;
import org.eclipse.cdt.core.dom.ast.IASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IType;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTUnaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexBinding;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Parameter;
import eu.synectique.verveine.core.gen.famix.ParameterizableClass;
import eu.synectique.verveine.core.gen.famix.ParameterizedType;
import eu.synectique.verveine.core.gen.famix.SourcedEntity;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.core.gen.famix.Type;
import eu.synectique.verveine.core.gen.famix.TypeAlias;
import eu.synectique.verveine.extractor.plugin.CDictionary;
import eu.synectique.verveine.extractor.utils.NullTracer;
//import eu.synectique.verveine.extractor.utils.Tracer;
import eu.synectique.verveine.extractor.utils.StubBinding;
import eu.synectique.verveine.extractor.utils.SubVisitorFactory;
import eu.synectique.verveine.extractor.visitors.AbstractVisitor;
import eu.synectique.verveine.extractor.visitors.SignatureBuilderVisitor;

public class RefVisitor extends AbstractRefVisitor implements ICElementVisitor {

	/**
	 * The source code of the visited AST.
	 * Used to find back the contents of non-javadoc comments
	 */
	protected RandomAccessFile source;

	/**
	 * Whether a variable access is lhs (write) or not
	 */
	protected boolean inAssignmentLHS = false;

	/**
	 * Constructor for "main" visitor, dicoDef contains entities created during def pass
	 * @param dicoDef contains entities created during def pass
	 * @param dicoRef where entities are created during ref pass (the current pass)
	 * @param index CDT index containing bindings
	 */
	public RefVisitor(CDictionary dico, IIndex index) {
		super(dico, index);

		tracer = new NullTracer("REF>");
	}

	protected String msgTrace() {
		return null;
	}


	/*
	 * redefining to check for header files (or not)
	 */
	@Override
	public void visit(ITranslationUnit elt) {
		if (checkHeader(elt)) {
			super.visit(elt);
		}
	}


	@Override
	protected int visit(IASTSimpleDeclaration node) {
		// compute nodeName and binding
		// also handles typedefs (see handleTypedef)
		// here we are interested in functions/methods declarations
		// to get the return type of the function

		if (declarationIsTypedef(node)) {
			for (IASTDeclarator declarator : node.getDeclarators()) {
			// this is a typedef, so the declarator(s) should be FAMIXType(s)

				nodeName = declarator.getName();

				nodeBnd = getBinding(nodeName);

				if (nodeBnd == null) {
					// create one anyway, assume this is a Type
					nodeBnd = StubBinding.getInstance(Type.class, dico.mooseName(context.getTopCppNamespace(), nodeName.toString()));
				}

				/* Call back method.
				 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
				 * thus several nodeName and bnd
				 */
				visitSimpleTypeDeclaration(node);
			}
			
			return PROCESS_SKIP;  // typedef already handled
		}

		return PROCESS_CONTINUE;
	}

	/*
	 * Visiting a class definition, need to put it on the context stack to create its members
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		// compute nodeName and binding
		super.visit(node);

		this.context.push((NamedEntity) returnedEntity);

		for (IASTDeclaration child : node.getDeclarations(/*includeInactive*/false)) {
			child.accept(this);
		}

		returnedEntity = this.context.pop();

		return PROCESS_SKIP;
	}

	/**
	 * Call back method from {@link #visit(IASTSimpleDeclaration)}
	 * sets the referedType for the AliasType of the typedef.
	 * Treated differently than other visit methods because, although unlikely, there could be more than one AliasType in the same typedef
	 * thus several nodeName and bnd.
	 */
	protected void visitSimpleTypeDeclaration(IASTSimpleDeclaration node) {
		TypeAlias fmx;

		fmx = (TypeAlias) dico.getEntityByKey(nodeBnd);

		node.getDeclSpecifier().accept(this);
		fmx.setAliasedType( (Type) returnedEntity );

		returnedEntity = fmx;
	}

	/**
	 * Visiting an attribute just to get it.
	 * {@link #visit(IASTSimpleDeclaration)} takes care of setting the return type
	 */
	@Override
	protected int visit(ICPPASTDeclarator node) {

		nodeBnd = null;
		nodeName = null;

		if (node.getParent() instanceof IASTSimpleDeclaration) {
			if ( nodeParentIsClass(node.getParent()) && ! declarationIsTypedef((IASTSimpleDeclaration)node.getParent()) ) {
				// this is an Attribute declaration, get it back
				nodeName = node.getName();

				nodeBnd = getBinding(nodeName);
				if (nodeBnd == null) {
					nodeBnd = StubBinding.getInstance(Attribute.class, dico.mooseName(context.topType(), nodeName.toString()));
				}
			}
		}


		if (node.getInitializer() != null ) {
			node.getInitializer().accept(this);
		}


		if (nodeBnd != null) {
			returnedEntity = (Attribute) dico.getEntityByKey(nodeBnd);
		}

		return PROCESS_SKIP;
	}

	@Override
	protected int visit(ICPPASTFunctionDeclarator node) {
		BehaviouralEntity fmx = null;

		// compute nodeName and binding
		super.visit(node);

		fmx = (BehaviouralEntity) dico.getEntityByKey(nodeBnd);
		// try harder
		if (fmx == null) {
			fmx = resolveBehaviouralFromName(node, nodeBnd);
		}

		this.context.push(fmx);
		// TODO remove not needed here anymore
		for (ICPPASTParameterDeclaration param : node.getParameters()) {
			param.accept(this);
		}
		returnedEntity = this.context.pop();

		return PROCESS_SKIP;  // already visited all we needed
	}

	@Override
	protected int visit(ICPPASTFunctionDefinition node) {
		BehaviouralEntity fmx = null;


		// visit declarator to ensure the method definition and to get the Famix entity
		returnedEntity = null;
		this.visit( (ICPPASTFunctionDeclarator)node.getDeclarator() );

		fmx = (BehaviouralEntity) returnedEntity;
		if (fmx != null) {
			this.context.push((NamedEntity) returnedEntity);

			for (ICPPASTConstructorChainInitializer init : node.getMemberInitializers()) {
				init.accept(this);
			}

			// to find out all accesses, references, invocations
			node.getBody().accept(this);
			
			returnedEntity = context.pop();
		}

		return PROCESS_SKIP;  // we already visited the children
	}

	@Override
	protected int visit(IASTUnaryExpression node) {
		node.getOperand().accept(this);
		return PROCESS_SKIP;
	}

}
