package eu.synectique.verveine.extractor.c;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ParserFactory;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.cdt.core.parser.util.ASTPrinter;
import org.eclipse.core.runtime.CoreException;

//import eu.synectique.licence.LicenceChecker;
import eu.synectique.verveine.core.VerveineParser;
import eu.synectique.verveine.core.gen.famix.CPPSourceLanguage;
import eu.synectique.verveine.core.gen.famix.SourceLanguage;

/**
 * A batch parser inspired from org.eclipse.jdt.internal.compiler.batch.Main (JDT-3.6)
 * run with:
 * java -cp lib/org.eclipse.jdt.core_3.6.0.v_A48.jar:../Fame:/usr/local/share/eclipse/plugins/org.eclipse.equinox.common_3.5.1.R35x_v20090807-1100.jar:/usr/local/share/eclipse/plugins/org.eclipse.equinox.preferences_3.2.301.R35x_v20091117.jar:/usr/local/share/eclipse/plugins/org.eclipse.core.jobs_3.4.100.v20090429-1800.jar:/usr/local/share/eclipse/plugins/org.eclipse.core.contenttype_3.4.1.R35x_v20090826-0451.jar:/usr/local/share/eclipse/plugins/org.eclipse.core.resources_3.5.2.R35x_v20091203-1235.jar:/usr/local/share/eclipse/plugins/org.eclipse.core.runtime_3.5.0.v20090525.jar:/usr/local/share/eclipse/plugins/org.eclipse.osgi_3.5.2.R35x_v20100126.jar:../Fame/lib/akuhn-util-r28011.jar:lib/fame.jar:bin eu.synectique.verveine.extractor.java.VerveineJParser [files|directory]_to_parse
 */

public class VerveineCParser extends VerveineParser {

	/**
	 * Whether to output all local variables (even those with primitive type or not (default is not).<br>
	 * Note: allLocals => not classSummary
	 */
	private boolean allLocals = false;

	/**
	 * The arguments that were passed to the parser
	 * Needed to relativize the source file names
	 */
	private Collection<String> argPath;
	private Collection<String> argFiles;

	public VerveineCParser() {
		super();
	}

	protected SourceLanguage getMyLgge() {
		return new CPPSourceLanguage();
	}

	public void setOptions(String[] args) {
		argPath = new ArrayList<String>();
		argFiles = new ArrayList<String>();

		int i = 0;
		while (i < args.length && args[i].trim().startsWith("-")) {
		    String arg = args[i++].trim();

			if (arg.equals("-h")) {
				usage();
			}
			else if (arg.equals("-alllocals")) {
				this.allLocals = true;
			}
			else {
				int j = super.setOption(i - 1, args);
				if (j > 0) {     // j is the number of args consumed by super.setOption()
					i += j;      // advance by that number of args
					i--;         // 1 will be added at the beginning of the loop ("args[i++]")
				}
				else {
					System.err.println("** Unrecognized option: " + arg);
					usage();
				}
			}
		}
		while (i < args.length) {
			String arg;
			arg = args[i++];
			if (arg.endsWith(".java") && new File(arg).isFile()) {
				argFiles.add(arg);
			} else {
				argPath.add(arg);
			}
		}
	}

	protected void usage() {
	
		System.err.println("Usage: VerveineC [-h] [-i] [-o <output-file-name>] [-summary] [-alllocals] [-anchor (none|default|assoc)] [-cp CLASSPATH | -autocp DIR] [-1.1 | -1 | -1.2 | -2 | ... | -1.7 | -7] <files-to-parse> | <dirs-to-parse>");
		System.err.println("      [-h] prints this message");
		System.err.println("      [-o <output-file-name>] specifies the name of the output file (default: output.mse)");
		System.err.println("      [-alllocals] Forces outputing all local variables, even those with primitive type (incompatible with \"-summary\"");
		System.err.println("      <files-to-parse>|<dirs-to-parse> list of source files to parse or directories to search for source files");
		System.exit(0);
	}

	public void parse() {
		String filename = "/home/anquetil/Documents/Synectique/projects/CParser/Code/mongo/src/mongo/db/cloner.h";

        IParserLogService log = ParserFactory.createDefaultLogService();

        Map<String,String> definedSymbols = new HashMap<String,String>();
        String[] includePaths = new String[0];
        IScannerInfo info = new ScannerInfo(definedSymbols,includePaths);
        IncludeFileContentProvider provider = IncludeFileContentProvider.getEmptyFilesProvider();
        FileContent reader = FileContent.createForExternalFileLocation(filename);
        
		try {
			IASTTranslationUnit ast;
			ast = GPPLanguage.getDefault().getASTTranslationUnit(reader, info, provider, null, 0, log);
	        ast.accept(new MainVisitor(new CppDictionary(getFamixRepo())));
		} catch (CoreException e) {
			System.err.println("Got CoreException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
		}
		catch (IllegalArgumentException e) {
			System.err.println("Got IllegalArgumentException (\""+ e.getMessage() +"\") while trying to parse: "+ filename );
		}
	}

}
