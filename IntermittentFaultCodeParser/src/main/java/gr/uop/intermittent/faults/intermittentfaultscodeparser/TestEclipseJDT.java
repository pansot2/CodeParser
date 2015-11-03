/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.uop.intermittent.faults.intermittentfaultscodeparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

/**
 *
 * @author Panos
 */
public class TestEclipseJDT {

    private int count = 0;
    private int count2 = 1;
    private HelloClass hello;

    public TestEclipseJDT() {
        count = 1;
        count2 = 2;
        hello = new HelloClass();
    }

    public static void parse(String str) {
        int count = 0;
        int c = count + 10;
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(str.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            Set names = new HashSet();
            Set methods = new HashSet();

            public boolean visit(FieldDeclaration node) {
                String name = node.fragments().get(0).toString().split("=")[0];
                String type = node.getType().toString();
                System.out.println("Field declaration '" + name + "' with type " + type + " at line "
                        + cu.getLineNumber(node.getStartPosition()));
                return false;
            }

            public boolean visit(MethodDeclaration node) {
                if (node.getName().getIdentifier() != null) {
                    System.out.println("Declaration of method : " + node.getName() + " with paremeters " + node.parameters().toString().split(",")[0] + " at line "
                            + cu.getLineNumber(node.getStartPosition()) + " is constructor " + node.isConstructor());
                    Block block = node.getBody();
                    Map<Integer, Set> variableNames = new HashMap();
                    blockIterate(block, cu, names, 0, variableNames);
                }
                return false;
            }
        });

    }

    private static void blockIterate(final Block block, final CompilationUnit cu, final Set fieldNames, final int blockNum, final Map<Integer, Set> variableNames) {
        List<Statement> statements = block.statements();
        System.out.println("Statements : " + statements.toString());
        
        Set variables = new HashSet();
        variableNames.put(blockNum, variables);
        
        for (Statement s : statements) {
            s.accept(new ASTVisitor() {
                Set names_ = new HashSet();

                public boolean visit(VariableDeclarationFragment node) {
                    SimpleName name = node.getName();
                    variableNames.get(blockNum).add(name.getIdentifier());
                    System.out.println("Declaration of variable '" + name + "' at line"
                            + cu.getLineNumber(name.getStartPosition())); 
                    return false; 
                }

                public boolean visit(SimpleName node) {
                    if (searchVariable(variableNames,node.getIdentifier())!=-1 || fieldNames.contains(node.getIdentifier())) {
                        System.out.println("Usage of variable/field '" + node.getIdentifier() + "' at line "
                                + cu.getLineNumber(node.getStartPosition()));
                    }
                    return false;
                }

                public boolean visit(MethodInvocation node) {
                    System.out.println("MethodInvocation: " + node.getName() + " at line "
                            + cu.getLineNumber(node.getStartPosition()) + " with arguments " + node.arguments());

                    Expression expression = node.getExpression();

                    if (expression != null) {
                    //    System.out.println("parent : " + expression.getParent().getParent());
                        System.out.println("Expr: " + expression.toString());
                    }
                    return false;
                }

                public boolean visit(Block node) {
                    if (node != null) {
                        System.out.println("Block " + node.toString());
                        int blockNumber = blockNum + 1;
                        blockIterate(node, cu, fieldNames, blockNumber,variableNames);
                    }
                    return false;
                }
            });
        }
    }
    
    private static int searchVariable(Map<Integer, Set> variableNames, String variable) {
        int blockSize = variableNames.size();
        
    //    System.out.println("BlockSize " + blockSize);
        
        for (int i=blockSize-1; i>=0; i--) {
            if (variableNames.get(i).contains(variable))
                System.out.println("Found in block : " + i);
                return i;
        }
        
        return -1;
    }

    //read file content into a string
    public static String readFileToString(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }

    //loop directory to get file list
    public static void ParseFilesInDir(String path) throws IOException {
        HelloClass h = new HelloClass();
        h.sayHello();
        File root = new File(path);
        File[] files = root.listFiles();
        String filePath = null;

        for (File f : files) {
            filePath = f.getAbsolutePath();
            if (f.isFile()) {
                System.out.println("Class : " + f.getName());
                parse(readFileToString(filePath));
            } else if (f.isDirectory()) {
                ParseFilesInDir(filePath);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File dirs = new File(".");
        String dirPath = dirs.getCanonicalPath() + File.separator + "src" + File.separator;
        ParseFilesInDir(dirPath);
    }

}
