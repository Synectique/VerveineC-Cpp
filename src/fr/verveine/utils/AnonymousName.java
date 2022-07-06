package fr.verveine.utils;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.ASTNodeProperty;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.ExpansionOverlapsBoundaryException;
import org.eclipse.cdt.core.dom.ast.IASTCompletionContext;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTImageLocation;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTNodeLocation;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.parser.IToken;

public class AnonymousName implements IASTName {
	private IASTName emptyIASTName;
	private String anonymousName;

	public AnonymousName(IASTName name, String nodeClass) {
		emptyIASTName= name;
		anonymousName = "[[Anonymous" + nodeClass + "]]";
	}

	@Override
	public boolean accept(ASTVisitor visitor) {
		return emptyIASTName.accept(visitor);
	}

	@Override
	public boolean contains(IASTNode node) {
		return emptyIASTName.contains(node);
	}

	@Override
	public IBinding getBinding() {
		return emptyIASTName.getBinding();
	}

	@Override
	public IBinding getPreBinding() {
		return emptyIASTName.getPreBinding();
	}

	@Override
	public String getContainingFilename() {
		return emptyIASTName.getContainingFilename();
	}

	@Override
	public IASTFileLocation getFileLocation() {
		return emptyIASTName.getFileLocation();
	}

	@Override
	public ILinkage getLinkage() {
		return emptyIASTName.getLinkage();
	}

	@Override
	public IASTNodeLocation[] getNodeLocations() {
		return emptyIASTName.getNodeLocations();
	}

	@Override
	public IASTNode getParent() {
		return emptyIASTName.getParent();
	}

	@Override
	public IASTNode[] getChildren() {
		return emptyIASTName.getChildren();
	}

	@Override
	public ASTNodeProperty getPropertyInParent() {
		return emptyIASTName.getPropertyInParent();
	}

	@Override
	public String getRawSignature() {
		return emptyIASTName.getRawSignature();
	}

	@Override
	public IASTTranslationUnit getTranslationUnit() {
		return emptyIASTName.getTranslationUnit();
	}

	@Override
	public int getRoleOfName(boolean allowResolution) {
		return emptyIASTName.getRoleOfName(allowResolution);
	}

	@Override
	public boolean isDeclaration() {
		return emptyIASTName.isDeclaration();
	}

	@Override
	public boolean isDefinition() {
		return emptyIASTName.isDefinition();
	}

	@Override
	public boolean isReference() {
		return emptyIASTName.isReference();
	}

	@Override
	public IBinding resolveBinding() {
		return emptyIASTName.resolveBinding();
	}

	@Override
	public IBinding resolvePreBinding() {
		return emptyIASTName.resolvePreBinding();
	}

	@Override
	public IASTCompletionContext getCompletionContext() {
		return emptyIASTName.getCompletionContext();
	}

	@Override
	public void setBinding(IBinding binding) {
		emptyIASTName.setBinding(binding);
	}

	@Override
	public void setParent(IASTNode node) {
		emptyIASTName.setParent(node);
	}

	@Override
	public void setPropertyInParent(ASTNodeProperty property) {
		emptyIASTName.setPropertyInParent(property);
	}

	@Override
	public char[] toCharArray() {
		return anonymousName.toCharArray();
	}

	@Override
	public String toString() {
		return anonymousName;
	}

	@Override
	public char[] getSimpleID() {
		return emptyIASTName.getSimpleID();
	}
	
	@Override
	public char[] getLookupKey() {
		return emptyIASTName.getLookupKey();
	}

	@Override
	public IASTImageLocation getImageLocation() {
		return emptyIASTName.getImageLocation();
	}

	@Override
	public boolean isPartOfTranslationUnitFile() {
		return emptyIASTName.isPartOfTranslationUnitFile();
	}

	@Override
	public IToken getLeadingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
		return emptyIASTName.getLeadingSyntax();
	}

	@Override
	public IToken getTrailingSyntax() throws ExpansionOverlapsBoundaryException, UnsupportedOperationException {
		return emptyIASTName.getTrailingSyntax();
	}

	@Override
	public IToken getSyntax() throws ExpansionOverlapsBoundaryException {
		return emptyIASTName.getSyntax();
	}

	@Override
	public boolean isFrozen() {
		return emptyIASTName.isFrozen();
	}

	@Override
	public boolean isActive() {
		return emptyIASTName.isActive();
	}

	@Override
	public IASTNode getOriginalNode() {
		return emptyIASTName.getOriginalNode();
	}

	@Override
	public IASTName getLastName() {
		return this;
	}

	@Override
	public IASTName copy() {
		return emptyIASTName.copy();
	}

	@Override
	public IASTName copy(CopyStyle style) {
		return emptyIASTName.copy(style);
	}

	@Override
	public boolean isQualified() {
		return false;
	}

}
