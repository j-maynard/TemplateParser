package com.punkyideas.templateparser;

import java.util.ArrayList;
import java.util.List;

public class SimpleParser {
	
	private static String START_BLOCK = "<!-- START-BLOCK";
	private static String END_BLOCK = "<!-- END-BLOCK -->";
	
    static Block blockParser(StringBuilder html) {
    	return blockParser(html.toString());
    }
	
	static Block blockParser(String html) {
		List<Block> currentNode = new ArrayList<>();
		String blockText = "";

		if (html.startsWith(START_BLOCK)) {
			blockText = START_BLOCK;
			html = html.substring(START_BLOCK.length());
		}

		while (html.length() > 0) {
			String startTestString = "";
			String endTestString = "";

			if (html.length() > START_BLOCK.length())
				startTestString = html.substring(0, START_BLOCK.length());
			if (html.length() > END_BLOCK.length())
				endTestString = html.substring(0, END_BLOCK.length());

			// Create a new node
			if (startTestString.equalsIgnoreCase(START_BLOCK)) {
				currentNode.add(Block.createNewBlock(blockText));
				blockText = "";
				Block childBlocks = blockParser(html);
				currentNode.add(childBlocks);
				html = html.substring(childBlocks.printDocument().length());
			}

			// End this block
			else if (endTestString.equalsIgnoreCase(END_BLOCK)) {
				if (currentNode.size() == 0)
					return Block.createNewBlock(blockText + END_BLOCK);
				currentNode.add(Block.createNewBlock(blockText + END_BLOCK));
				Block myBlock = Block.createNewNode(currentNode);
				myBlock.updateFamilyTree(null);
				return myBlock;
			}
			else {
				blockText += html.charAt(0);
				html = html.substring(1);
			}
		}
		currentNode.add(Block.createNewBlock(blockText));
		Block myBlock = Block.createNewNode(currentNode);
		myBlock.updateFamilyTree(null);
		return myBlock;
	}
	
}
