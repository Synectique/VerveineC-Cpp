package eu.synectique.verveine.extractor.cpp;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;

public class VerveineCParser {

	private IParserLogService log;
	private Map<String,String> definedSymbols;
	private String[] includePaths;
	private IScannerInfo info;
	private IncludeFileContentProvider provider;
	private IIndex indexer;
	
	Map<IBinding,IASTName> dico;

	public void parse() {
		dico = new HashMap<IBinding,IASTName>();
		
        log = ParserFactory.createDefaultLogService();
        definedSymbols = new HashMap<String,String>();
        includePaths = new String[0];
        info = new ScannerInfo(definedSymbols,includePaths);
        provider = IncludeFileContentProvider.getEmptyFilesProvider();
		ICProject project= CoreModel.getDefault().getCModel().getCProject("tmpmse");
		try {
			indexer= CCorePlugin.getIndexManager().getIndex(project);
		}
		catch (CoreException excp) {
			System.err.println(excp.getMessage());			
		}
		System.out.println("VerveineCParser created all helpers for project: " + project.getElementName());
		
		parseSourceFile("/home/anquetil/Documents/RMod/Tools/workspace/mongo/src/mongo/client/fetcher.h");
		parseSourceFile("/home/anquetil/Documents/RMod/Tools/workspace/mongo/src/mongo/client/fetcher.cpp");
	}

	private void parseSourceFile(String filename) {
        FileContent reader = FileContent.createForExternalFileLocation(filename);

        System.out.println("\n ---------- parsing: " + filename +" ----------");

		try {
			IASTTranslationUnit ast;
			ast = GPPLanguage.getDefault().getASTTranslationUnit(reader, info, provider, indexer, 0, log);
	        ast.accept(new MainVisitor(dico));
		} catch (CoreException e) {
			System.err.println("Got CoreException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
		}
		catch (IllegalArgumentException e) {
			System.err.println("Got IllegalArgumentException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
		}

	}
}
