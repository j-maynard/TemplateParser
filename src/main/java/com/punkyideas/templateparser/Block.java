package com.punkyideas.templateparser;

import java.util.List;

//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;

public class Block {
	
	private String html;
	private List<Block> children;
	private Block parent;
	private int contentCount = -2;
	//private Logger logger = LogManager.getLogger("TemplateParser");
	
	private Block(String html, List<Block> children) {
		this.html = html;
		this.children = children;
		this.parent = null;
	}

	public static Block createNewBlock(String html) {
		return new Block(html, null);
	}
	
	public static Block createNewNode(List<Block> children) {
		return new Block(null, children);
	}
	
	boolean isImplicitBlock() {
		return (children == null && html != null);
	}
	
	boolean hideBlock() {
		return false;
	}
	
	String printDocument() {
		if(hideBlock())
			return "";
		if(children == null)
			return html;
		
		StringBuilder resultStr = new StringBuilder();
		for(Block child : children) {
			resultStr.append(child.printDocument());
		}
		return resultStr.toString();
	}
	
	void parseHtml() {
		return;
	}
	
	// Override functions
	
	@Override
	public String toString() {
		if(this.children == null) 
			return "html = " + html + "\ncontentCount = " + contentCount;
		return "number of child nodes = " + children.size();
		
	}
	

	// Tree functions;
	void setParent(Block parent) {
		this.parent = parent;
	}
	
	void updateFamilyTree(Block parent) {
		this.parent = parent;
		if(this.children != null) {
			for(Block child : this.children) {
				child.updateFamilyTree(this);
			}
		}
	}
	
	String printTree() {
		return printTree(0);
	}
	
	private String printTree(int level) {
		String blockStr = "---Block\n";
		String nodeStr = "---Node\n";
		String levelStr = "   |";
		String tree = "|";
		for(int i = 0; i < level; i++) {
			tree += levelStr;
		}
		if(this.children == null) {
			tree += blockStr;
		} else {
			level++;
			tree += nodeStr;
			for(Block child: children) {
				tree += child.printTree(level);
			}
		}	
		return tree;
	}
	
	Block getParent() {
		return this.parent;
	}
	
	int getBranceSize() {
		
		if(children == null) {
			return 1;
		}
		
		int size = 0;
		for(Block block : children) {
			size += block.getTreeSize();
		}
		
		return size +1;
	}
	
	int getTreeSize() {
		return getRoot().getBranceSize();
	}
	
	Block getRoot() {
		if(this.parent == null)
			return this;
		return parent.getRoot();
	}

}
