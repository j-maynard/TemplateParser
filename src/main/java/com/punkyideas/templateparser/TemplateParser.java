package com.punkyideas.templateparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;

public class TemplateParser {

	private static final String START_BLOCK = "<!-- START-BLOCK";
	private static final String END_BLOCK = "<!-- END-BLOCK -->";

	private File templateFile;
	private StringBuilder template;
	private Block templateTree;

	private TemplateParser(File templateFilePath) {
		this.templateFile = templateFilePath;

		// Open template file and place in a string
		try (BufferedReader br = new BufferedReader(new FileReader(templateFile.getPath()))) {
			template = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				template.append(line);
				template.append(System.lineSeparator());
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			logger.fatal(e);
			System.exit(1);
		} catch (IOException e) {
			logger.fatal(e);
			System.exit(1);
		}

		logger.debug("Input HTML File:\n" + template);

		templateTree = blockParser3(template);

	}
	
	Block getTemplateTree() {
		return this.templateTree;
	}
	
	/**
	 * Starting point for parsing a document looking for
	 * the value of START_BLOCK and END_BLOCK and building
	 * a tree of blocks from it.
	 * @param html
	 * @return
	 */
	Block blockParser3(StringBuilder html) {
		try {
			return blockParser3(html, "");
		} catch(ParseException e) {
			logger.fatal("Unable to parse the document due to an error in the document", e);
			System.exit(1);
			return null;
		}
	}
	
	private int currentCharIndex = 0;
	private int level = 0;
	
	/** 
	 * Private method should not be used directly.  Instead please call
	 * blockParser3(StringBuilder html)
	 * @param html
	 * @param initalIdentifier
	 * @return
	 * @throws ParseException
	 */
    private Block blockParser3(StringBuilder html, String initalIdentifier) throws ParseException{
		List<Block> currentNode = new ArrayList<>();
		StringBuilder blockText = new StringBuilder();
		
		int startBlockCharIndex = currentCharIndex;
		
		blockText.append(initalIdentifier);
		
		while (html.length() > 0) {
			String startTestString = "";
			String endTestString = "";
			
			// Test the incoming string for length before
			// setting the value of the test strings
			if (html.length() > START_BLOCK.length())
				startTestString = html.substring(0, START_BLOCK.length());
			if (html.length() > END_BLOCK.length())
				endTestString = html.substring(0, END_BLOCK.length());

			// Test to see if we're at the start of a block
			if (startTestString.equalsIgnoreCase(START_BLOCK)) {
				// If we are increase the level and create a new block
				// from the current value of blockText
				level++;
				currentNode.add(Block.createNewBlock(blockText.toString()));
				blockText = new StringBuilder();
				// remove the start_block from the string bieng parsed
				html.delete(0, START_BLOCK.length());
				currentCharIndex += START_BLOCK.length();
				// Call ourselves and being to parse again
				Block childBlocks = blockParser3(html, START_BLOCK);
				currentNode.add(childBlocks);
			}
			// Test for the end of a block
			else if (endTestString.equalsIgnoreCase(END_BLOCK)) {
				level--;
				// Remove the END_BLOCK text from the parsed html string before returning
				// other wise we'll drop out early.
				html.delete(0, END_BLOCK.length());
				currentCharIndex += END_BLOCK.length();
				
				// catch mismatched END_BLOCK instances
				if(level < 0) {
					throw new ParseException("End block without matching start block at index " + (currentCharIndex - START_BLOCK.length()));
				}
				
				// If we haven't created any other blocks in this instance
				// just return a block with the value of block text with its END_BLOCK
				if (currentNode.isEmpty())
					return Block.createNewBlock(blockText.toString() + END_BLOCK);
				
				// Otherwise we need to add the value of block text to the current node
				// and return that.
				currentNode.add(Block.createNewBlock(blockText.toString() + END_BLOCK));
				Block myBlock = Block.createNewNode(currentNode);
				myBlock.updateFamilyTree(null);
				return myBlock;
			}
			else {
				// Keep moving through the text until we reach the end of the document
				blockText.append(html.substring(0, 1));
				html.deleteCharAt(0);
				currentCharIndex++;
			}
		}
		
		// IF we reach the end of the document without getting back to level 0 we have miss matched START and END BLOCKS
		if(level > 0) {
			throw new ParseException("Start block without matching end blcok at index " + (startBlockCharIndex - END_BLOCK.length()));
		}
		
		// This is unlikely but possible.  No START and END BLOCKS we just return the whole document
		// as a single block
		if (currentNode.isEmpty())
			return Block.createNewBlock(blockText.toString());
		
		// create a new block with the remain text in blockText
		// Add it to the current node and return 
		currentNode.add(Block.createNewBlock(blockText.toString()));
		Block myBlock = Block.createNewNode(currentNode);
		myBlock.updateFamilyTree(null);
		return myBlock;
	}

	// Just used to start the parser
	private static Logger logger;

	public static void main(String[] args) {
		
		String logPath = "/tmp"; // Linux, Mac
		// String logPath = "c:\\Users\\admin\\Desktop"; // Windows

		String logLevel = "debug";

		String templateFilePath = "/jamie/Documents/testTemplate.html"; // Linux
		// String templateFilePath = "/Users/jamie/Development/templateProject/testTemplate.html"; // Mac
		// String templateFilePath = "c:\\Users\\admin\\Desktop\\testTemplate.html"; // Windows

		// Parse command line options
		final CommandLineParser optionsParser = new DefaultParser();
		Options options = constructOptions();
		CommandLine commandLine;
		try {
			commandLine = optionsParser.parse(options, args);
			if (commandLine.hasOption("t") || commandLine.hasOption("template")) {
				logPath = commandLine.getOptionValue("L");
			} /*
				 * else { System.out.
				 * println("Template file is missing and is required.");
				 * displayHelp(options); }
				 */
			if (commandLine.hasOption("l") || commandLine.hasOption("logpath")) {
				logPath = commandLine.getOptionValue("L");
			}
			if (commandLine.hasOption("L") || commandLine.hasOption("loglevel")) {
				logLevel = commandLine.getOptionValue("L");
			}
			if (commandLine.hasOption("h") || commandLine.hasOption("help")) {
				displayHelp(options);
			}
		} catch (ParseException e) {
			System.out.println("Encountered an error trying to parse the commandline options\n");
			e.printStackTrace();
			System.exit(1);
		}

		// Start log4j
		setupLogger(logPath, logLevel);

		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		FileAppender logFile = (FileAppender) config.getAppender("MyFile");

		logger.info("Using " + logFile.getFileName() + " for the log file");
		logger.info("Log path = " + logPath);

		File templateFile = new File(templateFilePath);
		TemplateParser templateParser = new TemplateParser(templateFile);
		Block templateTree = templateParser.getTemplateTree();
		
		//logger.debug("Root node to String:\n" + templateTree.toString());
		logger.debug("printDocument : \n" + templateTree.printDocument());
		logger.debug("printTree : \n" + templateTree.printTree());
	}

	private static void setupLogger(String logPath, String logLevel) {
		System.setProperty("log4j.configurationFile", "log4j2.xml");
		System.setProperty("log.level", logLevel);
		System.setProperty("log.path", logPath);
		logger = LogManager.getLogger("TemplateParser");

		// Advise which log level is used:
		switch (logger.getLevel().getStandardLevel()) {
		case TRACE:
			logger.trace("Log4J Logger Loaded");
			logger.trace("Logging level set to Trace");
			break;
		case ALL:
			logger.info("Log4J Logger Loaded");
			logger.info("Logging level set to All");
			break;
		case DEBUG:
			logger.debug("Log4J Logger Loaded");
			logger.debug("Logging level set to Debug");
			break;
		case ERROR:
			logger.error("Log4J Logger Loaded");
			logger.error("Logging level set to Error");
			break;
		case FATAL:
			logger.fatal("Log4J Logger Loaded");
			logger.fatal("Logging level set to Fatal");
			break;
		case INFO:
			logger.info("Log4J Logger Loaded");
			logger.info("Logging level set to Info");
			break;
		case WARN:
			logger.warn("Log4J Logger Loaded");
			logger.warn("Logging level set to Warn");
			break;
		case OFF:
			break;
		}
	}

	private static Options constructOptions() {
		final Options options = new Options();
		options.addOption("t", "template", true, "The path of the template file to parse (required)");
		options.addOption("l", "logpath", true, "The path for log4j files to be written to (Default /tmp)");
		options.addOption("L", "loglevel", true,
				"Sets the log level. (Default: info) [trace, all, debug, error, fatal, info, warn, off]");
		options.addOption("h", "help", false, "Display help information and exits");
		return options;
	}

	private static void displayHelp(Options options) {
		String commandLineSyntax = "java -jar TemplateParser.jar";
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.printHelp(commandLineSyntax, options);
		System.exit(0);
	}

}
