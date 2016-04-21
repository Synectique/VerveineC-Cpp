package eu.synectique.verveine.extractor.ref;

import java.io.File;
import java.io.RandomAccessFile;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTComment;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTDoStatement;
import org.eclipse.cdt.core.dom.ast.IASTEnumerationSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTIfStatement;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.core.dom.ast.IASTWhileStatement;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IProblemBinding;
import org.eclipse.cdt.core.dom.ast.c.ICASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNamespaceDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTRangeBasedForStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTryBlockStatement;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPBase;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ICContainer;
import org.eclipse.cdt.core.model.ICElementVisitor;
import org.eclipse.cdt.core.model.ITranslationUnit;

import eu.synectique.verveine.core.Dictionary;
import eu.synectique.verveine.core.EntityStack2;
import eu.synectique.verveine.core.gen.famix.Access;
import eu.synectique.verveine.core.gen.famix.Association;
import eu.synectique.verveine.core.gen.famix.Attribute;
import eu.synectique.verveine.core.gen.famix.BehaviouralEntity;
import eu.synectique.verveine.core.gen.famix.Class;
import eu.synectique.verveine.core.gen.famix.Function;
import eu.synectique.verveine.core.gen.famix.Inheritance;
import eu.synectique.verveine.core.gen.famix.Invocation;
import eu.synectique.verveine.core.gen.famix.Method;
import eu.synectique.verveine.core.gen.famix.NamedEntity;
import eu.synectique.verveine.core.gen.famix.Namespace;
import eu.synectique.verveine.core.gen.famix.Package;
import eu.synectique.verveine.core.gen.famix.StructuralEntity;
import eu.synectique.verveine.extractor.def.CDictionaryDef;
import eu.synectique.verveine.extractor.utils.NullTracer;

public class MainRefVisitor extends AbstractRefVisitor implements ICElementVisitor {

	/**
	 * A stack that keeps the current definition context (package/class/method)
	 */
	protected EntityStack2 context;

	/**
	 * The source code of the visited AST.
	 * Used to find back the contents of non-javadoc comments
	 */
	protected RandomAccessFile source;

	/**
	 * Whether a variable access is lhs (write) or not
	 */
	protected boolean inAssignmentLHS = false;

	protected eu.synectique.verveine.core.gen.famix.Package currentPackage = null;

	private CDictionaryDef dicoDef;

	private NamedEntity lastExpr;
	
	public MainRefVisitor(CDictionaryDef dicoDef, CDictionaryRef dicoRef, IIndex index) {
		super(dicoRef, index, /*visitNodes*/true);
		this.dicoDef = dicoDef;

		tracer = new NullTracer("REF>");
	}

	// VISITING METODS ON ICELEMENT HIERARCHY ======================================================================================================

	@Override
	public void visit(ITranslationUnit tu) {
		tracer.up("ITranslationUnit: "+tu.getElementName());
		context = new EntityStack2();    // "reseting" context
		super.visit(tu);
		tracer.down();
	}

	/**
	 * Removes the package corresponding to elt from dicoDef.<BR>
	 * No need to put it in dicoRef as it is not used here
	 */
	@Override
	public void visit(ICContainer elt) {
		eu.synectique.verveine.core.gen.famix.Package fmx = (Package) dicoDef.removeScopingEntity(elt.getElementName(), currentPackage);

		currentPackage = fmx;                        // kind of pushing new package on a virtual package stack
		super.visit(elt);                            // visit container children
		currentPackage = fmx.getParentPackage();    // kind of popping out the new package from the package stack
	}


	// CDT VISITING METODS ON AST ==========================================================================================================

	@Override
	public int visit(ICPPASTNamespaceDefinition node) {
		tracer.up("ICPPASTNamespaceDefinition: "+node.getName());
		IASTName nodeName = node.getName();
		IBinding bnd = nodeName.resolveBinding();

		if (bnd == null) {
			return PROCESS_CONTINUE;
		}

		Namespace fmx = (Namespace) dicoDef.removeScopingEntity(nodeName.getLastName().toString(), (Namespace)context.top());
		if (fmx == null) {
			return PROCESS_CONTINUE;
		}
		dico.remapEntityToKey(nodeName.resolveBinding(), fmx);		
		this.context.push(fmx);

		return super.visit(node);
	}

	@Override
	public int leave(ICPPASTNamespaceDefinition node) {
		this.context.pop();
		tracer.down();
		return super.leave(node);
	}

	@Override
	public int visit(IASTParameterDeclaration node) {
/*		tracer.msg("IASTParameterDeclaration: ");
		if (context.topMethod() != null) {
			node.accept( new ParamDeclVisitor(dico, context.topMethod()) );
		}*/
		return super.visit(node);
	}

	@Override
	public int visit(IASTDeclaration node) {
		// includes CPPASTVisibilityLabel
//		tracer.msg("IASTDeclaration");
		if (node instanceof ICPPASTFunctionDefinition) {
			return visit((ICPPASTFunctionDefinition)node);
		}
		return super.visit(node);
	}

	@Override
	public int leave(IASTDeclaration node) {
		if (node instanceof ICPPASTFunctionDefinition) {
			return leave((ICPPASTFunctionDefinition)node);
		}
		return super.leave(node);
	}

	@Override
	public int visit(IASTStatement node) {
		// IASTBreakStatement, IASTCaseStatement, IASTCompoundStatement, IASTContinueStatement, IASTDeclarationStatement, IASTDefaultStatement,
		// IASTDoStatement, IASTExpressionStatement, IASTForStatement, IASTGotoStatement, IASTIfStatement, IASTLabelStatement, IASTNullStatement,
		// IASTProblemStatement, IASTReturnStatement, IASTSwitchStatement, IASTWhileStatement, ICPPASTCatchHandler, ICPPASTCompoundStatement,
		// ICPPASTForStatement, ICPPASTIfStatement, ICPPASTRangeBasedForStatement, ICPPASTSwitchStatement, ICPPASTTryBlockStatement, ICPPASTWhileStatement,
		// IGNUASTGotoStatement
	    if ( (node instanceof IASTCaseStatement)			||
	    	 (node instanceof IASTDefaultStatement)			||
	    	 (node instanceof IASTDoStatement)				||
	    	 (node instanceof IASTForStatement)				||
	    	 (node instanceof IASTIfStatement)				||
	    	 (node instanceof IASTWhileStatement)			||
	    	 (node instanceof ICPPASTRangeBasedForStatement)||
	    	 (node instanceof ICPPASTTryBlockStatement)		)  {
	    	context.setTopMethodCyclo( context.getTopMethodCyclo() + 1);
	    }
	    
	    if ( (node instanceof IASTCaseStatement)	||
		     (node instanceof IASTCompoundStatement)||
		     (node instanceof IASTDefaultStatement)	||
		     (node instanceof IASTLabelStatement)	||
		     (node instanceof IASTNullStatement)	) {
	    	// nothing, I was too lazy to negate all the above boolean expressions :-)
	    }
	    else {
	    	context.setTopMethodNOS( context.getTopMethodNOS() + 1);
	    }

		return super.visit(node);
	}

	@Override
	public int visit(IASTExpression node) {
		if (node instanceof IASTFieldReference) {
			// can also be a method invocation
			((IASTFieldReference)node).getFieldOwner().accept(this);
			((IASTFieldReference) node).getFieldName().accept(this);
			return ASTVisitor.PROCESS_SKIP;
		}
		else if (node instanceof IASTFunctionCallExpression) {
			visit((IASTFunctionCallExpression)node);
			return ASTVisitor.PROCESS_CONTINUE;  // should be SKIP because we already visited the FunctionNameExpression?
		}
		else if (node instanceof IASTIdExpression) {
			referenceToName(((IASTIdExpression) node).getName());
			return ASTVisitor.PROCESS_SKIP;
		}
		else if (node instanceof IASTBinaryExpression) {
			visit((IASTBinaryExpression)node);   // to check whether this is an assignement
			return ASTVisitor.PROCESS_SKIP;
		}
		else if (node instanceof IASTLiteralExpression) {
			if ( ((IASTLiteralExpression)node).getKind() == ICPPASTLiteralExpression.lk_this ) {
				if (context.topType() != null) {
					accessToVar(dico.ensureFamixImplicitVariable(Dictionary.SELF_NAME, /*type*/context.topType(), /*owner*/context.topMethod(), /*persistIt*/true));
				}
			}
			return ASTVisitor.PROCESS_SKIP;
		}

		return super.visit(node);
	}

	@Override
	public int visit(IASTName node) {
		referenceToName(((IASTName) node).getLastName());
		return ASTVisitor.PROCESS_SKIP;
	}

	@Override
	public int visit(IASTComment node) {
//		tracer.up("IASTComment "+node.toString());
		return super.visit(node);
	}

	@Override
	public int leave(IASTComment node) {
//		tracer.down();
		return super.leave(node);
	}


	// ADDITIONAL VISITING METODS ON AST ==================================================================================================

	/** Visiting a class definition
	 */
	@Override
	protected int visit(ICPPASTCompositeTypeSpecifier node) {
		IASTName nodeName;
		IASTFileLocation loc;
		IBinding bnd;
		Class fmx;

		nodeName = node.getName();
		loc = node.getFileLocation();
		if ( (nodeName == null) || (loc == null) ) {
			return PROCESS_CONTINUE;
		}

		bnd = nodeName.resolveBinding();
		if (bnd == null) {
			return PROCESS_CONTINUE;
		}

		if (dico.getEntityByKey(bnd) != null) {
			// already visited, everything done
			return PROCESS_CONTINUE;
		}

		tracer.up("ICPPASTCompositeTypeSpecifier:"+nodeName.toString());

		fmx = dicoDef.removeEntity(loc.getFileName(), loc.getNodeOffset(), nodeName.toString(), eu.synectique.verveine.core.gen.famix.Class.class);
		if (fmx == null) {
			return PROCESS_CONTINUE;
		}

		dico.remapEntityToKey(nodeName.resolveBinding(), fmx);

		// For classes, we create the anchor now
		dico.addSourceAnchor(fmx, filename, loc);
		fmx.setIsStub(false);
		ICPPClassType classBnd = (ICPPClassType)bnd;
		Inheritance lastInheritance = null;
		for (ICPPBase superBnd : classBnd.getBases()) {
			eu.synectique.verveine.core.gen.famix.Class sup = dico.ensureClass(superBnd.getBaseClass(), superBnd.getBaseClassSpecifierName().toString());
			lastInheritance = dico.ensureFamixInheritance(sup, fmx, lastInheritance);
		}

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(ICPPASTCompositeTypeSpecifier node) {
		context.pop();
		tracer.down();

		return PROCESS_CONTINUE;		
	}

	@Override
	protected int visit(IASTFunctionDeclarator node) {
		IASTName nodeName;
		IASTFileLocation loc;
		IBinding bnd;
		BehaviouralEntity fmx = null;

		nodeName = node.getName();
		loc = node.getParent().getFileLocation();
		if ( (nodeName == null) || (loc == null) ){
			return PROCESS_CONTINUE;
		}

		bnd = nodeName.resolveBinding();
		if (bnd==null) {
			return PROCESS_CONTINUE;
		}

		if (dico.getEntityByKey(bnd) != null) {
			// already visited, everything done
			return PROCESS_CONTINUE;
		}
		tracer.msg("IASTFunctionDeclarator: "+nodeName);

		/* Note that, below, 'loc.getFileName()' can be different from 'this.filename', if the later is a .cpp file and
		 * the method is defined in an included .hpp file*/
		if (bnd instanceof ICPPMethod) {   // C++ method
			fmx = dicoDef.removeEntity(loc.getFileName(), loc.getNodeOffset(), nodeName.toString(), Method.class);
		}
		else {                    //   C function ?
			fmx = dicoDef.removeEntity(loc.getFileName(), loc.getNodeOffset(), nodeName.toString(), Function.class);
		}

		if (fmx == null) {
			return PROCESS_CONTINUE;
		}

		dico.remapEntityToKey(bnd, fmx);
		// for methods, sourceAnchor is set in the definition, that is to say not here but in visit(ICPPASTFunctionDefinition node)

		fmx.setSignature(bnd.toString());

		this.context.push(fmx);

		return PROCESS_CONTINUE;
	}

	@Override
	protected int leave(IASTFunctionDeclarator node) {
		NamedEntity top = context.top();
		if ( (top != null) &&
			 (top instanceof BehaviouralEntity) &&
			 (top.getName().equals(node.getName().toString())) ) {
			context.pop();
		}
		return ASTVisitor.PROCESS_CONTINUE;
	}

	protected int visit(ICPPASTFunctionDefinition node) {
		IASTName nodeName;
		IASTFileLocation loc;
		IBinding bnd;
		BehaviouralEntity fmx = null;

		nodeName = node.getDeclarator().getName();
		loc = node.getFileLocation();
		if (nodeName == null) {
			return super.visit(node);
		}
		tracer.msg("ICPPASTFunctionDefinition: "+nodeName);

		bnd = nodeName.resolveBinding();
		if (bnd!=null) {
			fmx = (BehaviouralEntity) dico.getEntityByKey(bnd);
			if ( (fmx == null) && (! (bnd instanceof IProblemBinding)) ) {
				/*
				 * may be we did not yet visit the FunctionDeclarator?
				 * that may happen if the function is declared and defined at the same time
				 * But this would be useless if we could not resolve the binding
				 */
				visit(node.getDeclarator());   // will push the declarator on the context stack
				fmx = (BehaviouralEntity) dico.getEntityByKey(bnd);
				leave(node.getDeclarator());   // to pop the declarator from the context stack
			}

			if ( (fmx != null) && (loc != null) ) {
				dico.addSourceAnchor(fmx, loc.getFileName(), loc);
			}
		}

		// now visiting the children of the node
		this.context.push(fmx);
		node.getDeclSpecifier().accept(this);
		context.setLastAccess(null);
		context.setLastInvocation(null);
		context.setLastReference(null);
		context.setTopMethodCyclo(0);
		context.setTopMethodNOS(0);
		node.getBody().accept(this);
		this.context.pop();

		return ASTVisitor.PROCESS_SKIP;  // we already visited the children
	}

	protected int leave(ICPPASTFunctionDefinition node) {
		return super.leave(node);
	}


	@Override
	protected int visit(ICPPASTDeclarator node) {
		IASTName nodeName = null;
		IASTFileLocation loc = null;
		Attribute fmx = null;

		nodeName = node.getName();
		/*
		 * For ICPPASTDeclarator, the location must be that of the parent, i.e. the declaration
		 * For example, in "int a,b;" the declaration starts at "int" whereas there are 2 declarators: a and b
		 */
		loc = node.getParent().getFileLocation();
		if ( (nodeName == null) || (loc == null) ) {
			return PROCESS_CONTINUE;
		}

		fmx = dicoDef.removeEntity(loc.getFileName(), loc.getNodeOffset(), nodeName.toString(), Attribute.class);
		if (fmx == null) {
			return PROCESS_CONTINUE;
		}

		dico.remapEntityToKey(nodeName.resolveBinding(), fmx);
		dico.addSourceAnchor(fmx, filename, loc);

		return PROCESS_CONTINUE;
	}


	@Override
	protected int leave(ICPPASTDeclarator node) {
		return PROCESS_CONTINUE;
	}


	public void visit(IASTFunctionCallExpression node) {
/*		IASTName nodeName = null;
		BehaviouralEntity fmx = null;
		IBinding bnd;

		nodeName = node.getFunctionNameExpression();
		if (nodeName != null) {
			bnd = nodeName.resolveBinding();
			fmx = (Attribute) dico.getEntityByKey(bnd);
			if (fmx != null) {
				IASTImageLocation loc = nodeName.getImageLocation();
				tracer.msg("IASTFunctionCallExpression to:"+fmx.getName()+" @ "+loc.getFileName()+"/"+loc.getStartingLineNumber());
				lastExpr = fmx;
			}
		}*/
	}


	public void visit(IASTBinaryExpression node) {
		Access prevAccess = context.getLastAccess();
		node.getOperand1().accept(this);
		switch (node.getOperator()) {
		case IASTBinaryExpression.op_assign:
		case IASTBinaryExpression.op_binaryAndAssign:
		case IASTBinaryExpression.op_binaryOrAssign:
		case IASTBinaryExpression.op_binaryXorAssign:
		case IASTBinaryExpression.op_divideAssign:
		case IASTBinaryExpression.op_minusAssign:
		case IASTBinaryExpression.op_moduloAssign:
		case IASTBinaryExpression.op_multiplyAssign:
		case IASTBinaryExpression.op_plusAssign:
		case IASTBinaryExpression.op_shiftLeftAssign:
		case IASTBinaryExpression.op_shiftRightAssign:
			if (context.getLastAccess() != prevAccess) {
				context.getLastAccess().setIsWrite(true);
			}
		}
		node.getOperand2().accept(this);
	}


	@Override
	protected int visit(ICASTCompositeTypeSpecifier node) {
//		tracer.msg("    -> ICASTCompositeTypeSpecifier");
		return PROCESS_CONTINUE;
	}


	@Override
	protected int leave(ICASTCompositeTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}


	@Override
	protected int visit(IASTEnumerationSpecifier node) {
//		tracer.msg("    -> IASTEnumerationSpecifier");
		return PROCESS_CONTINUE;
	}


	@Override
	protected int leave(IASTEnumerationSpecifier node) {
		return PROCESS_CONTINUE;
	}


	@Override
	protected int visit(ICPPASTNamedTypeSpecifier node) {
//		tracer.msg("    -> ICPPASTNamedTypeSpecifier");
		return PROCESS_CONTINUE;
	}


	@Override
	protected int leave(ICPPASTNamedTypeSpecifier node) {
		return PROCESS_CONTINUE;
	}
	

	/**
	 * Records a reference to a name which can be a variable or behavioral name.
	 * @param nodeName
	 * @return the Access or Invocation created
	 */
	// UTILITIES ==============================================================================================================================

	private Association referenceToName(IASTName nodeName) {
		IBinding bnd;
		NamedEntity fmx;

		if (nodeName == null) {
			return null;
		}

		bnd = nodeName.resolveBinding();
		if (bnd == null) {
			return null;
		}

		fmx = dico.getEntityByKey(bnd);
		if (fmx == null) {
			return null;
		}

		if (fmx instanceof StructuralEntity) {
			return accessToVar((StructuralEntity) fmx);
		}
		else if (fmx instanceof BehaviouralEntity) {
			return invocationOfBehavioural((BehaviouralEntity) fmx);
		}

		return null;
	}

	/**
	 * Records an Invocation of a famixBehaviouralEntity and sets lastInvocation attribute
	 * @param fmx
	 * @return the invocation created
	 */
	private Association invocationOfBehavioural(BehaviouralEntity fmx) {
		BehaviouralEntity accessor = this.context.topMethod();
		Invocation invok = dico.addFamixInvocation(accessor, (BehaviouralEntity) fmx, /*receiver*/null, /*signature*/null, context.getLastInvocation());
		if (invok != null) {
			context.setLastInvocation(invok);
		}
		return invok;
	}

	/**
	 * Records an Access to a StructuralEntity and sets lastAccess attribute
	 * @param fmx
	 * @return the Access created
	 */
	private Association accessToVar(StructuralEntity fmx) {
		BehaviouralEntity accessor;
		// put false to isWrite by default, will be corrected in 
		accessor = this.context.topMethod();
		Access acc = dico.addFamixAccess(accessor, (StructuralEntity) fmx, /*isWrite*/false, context.getLastAccess());
		if (acc != null) {
			context.setLastAccess(acc);
		}
		return acc;
	}


	protected void tracename(IASTName name) {
		if (name != null) {
			tracer.msg("    -> '"+name.toString()+ "' " +(name.resolveBinding()!=null ?"is":"not") +" bound");
		}
		else {
			tracer.msg("    -> null name");
		}
	}


	protected void traceanchor(IASTFileLocation loc) {
		if (loc != null) {
			String filename = loc.getFileName();
			filename = filename.substring(filename.lastIndexOf(File.separatorChar)+1);
			tracer.msg("    -> fileanchor=<"+filename+","+loc.getNodeOffset()+">");
		}
		else {
			tracer.msg("    -> fileanchor=<null>");
		}
	}

}
