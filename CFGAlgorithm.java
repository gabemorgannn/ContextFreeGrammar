/**
 * In this assignment you will get hands-on practice with designing grammars, and then designing programs 
 * that can use a grammar to parse and filter strings. This project will also reinforce what you have been
 * learning about data structures and algorithms in your other classes.
 * 
 * @author Matthew Swandal, Last Modified 4/10/25
 * @author Gabriel Morgan, Last Modified 4/10/25
 */

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CFGAlgorithm {

    static ArrayList<String> variables = new ArrayList<>(); // non-terminals 
    static ArrayList<String> terminals = new ArrayList<>(); // terminals 
    static ArrayList<ArrayList<String>> rules = new ArrayList<>(); // oroduction rules
    static ArrayList<String> ruleVariables = new ArrayList<>(); // LHS variable for each rule
    static String startVariable = ""; // start variable set to empty cause unknown at start

    public static void main(String[] args) throws IOException {
        // checks for 2 command line arguments when testing the grammar file and input file
        if (args.length != 2) {
            System.out.println("Usage: java CFGTest <grammarFile> <inputFile>");
            return;
        }
        // parsing the grammar file
        parseGrammar(args[0]);
        // print grammar for debug
        debugGrammar();
        //test input
        testInputStrings(args[1]);
    }

    /**
     * parses the grammr from the given file.
     * identifies variables, terminals, rules, and the start variable.
     *
     * @param grammarFile file containing the grammar.
     * @throws IOException if file reading fails.
     */
    //https://lazeecoder.medium.com/event-driven-context-free-grammar-cfg-parsing-66298c0f1ef7  gave us idea for LHS and RHS
    static void parseGrammar(String grammarFile) throws IOException {
        Scanner scanner = new Scanner(new File(grammarFile));
        int currentLine = 1;
        // loop through the file until no more lines
        while (scanner.hasNextLine()) {
            // the following if statements define the variables, terminals, start
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (currentLine == 1) {
                variables.addAll(Arrays.asList(line.split(","))); // get variables. https://docs.oracle.com/javase/8/docs/api/java/util/Arrays.html for asList
            } else if (currentLine == 2) {
                terminals.addAll(Arrays.asList(line.split(","))); // get terminals.
            } else if (!line.contains("->")) {
                startVariable = line; // identify start.
                //else breaks down grammar into it's components
            } else {
                String[] ruleSplit = line.split("->"); // https://www.w3schools.com/java/ref_string_split.asp 
                String lhs = ruleSplit[0].trim();
                String[] rhsComponents = ruleSplit[1].trim().split("\\|"); // https://www.w3schools.com/java/ref_string_trim.asp 

                int lhsIndex = variables.indexOf(lhs);
                if (lhsIndex == -1) { // https://www.w3schools.com/java/ref_string_indexof.asp 
                    variables.add(lhs);
                    lhsIndex = variables.size() - 1;
                }

                for (String rhs : rhsComponents) {
                    ArrayList<String> production = new ArrayList<>(Arrays.asList(rhs.trim().split("\\s+"))); // Split by spaces
                    rules.add(production);
                    ruleVariables.add(lhs); // map LHS variable to the rule.
                }
            }
            currentLine++;
        }
        scanner.close();
    }

    /**
     * prints the parsed grammar to ensure that the grammar is being interpretted correctly.
     */
    static void debugGrammar() {
        System.out.println("Variables: " + String.join(", ", variables));
        System.out.println("Terminals: " + String.join(", ", terminals));
        System.out.println("Rules:");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println(ruleVariables.get(i) + " -> " + String.join(" | ", rules.get(i)));
        }
        System.out.println("Start Variable: " + startVariable);
        System.out.println();
    }

    /**
     * gets all rules for a given variable.
     *
     * @param variable the variable.
     * @return list of rules.
     */
    static ArrayList<String> getRulesForVariable(String variable) {
        ArrayList<String> result = new ArrayList<>();
        // loop through from i and gets all the rules and stores in an array list rule.
        for (int i = 0; i < rules.size(); i++) {
            if (ruleVariables.get(i).equals(variable)) {
                result.addAll(rules.get(i));
            }
        }
        return result;
    }

    /**
     * implements Sipser algorithm to check string.
     *
     * @param inputString string to check.
     * @return true if the string is in the language else false
     */
    static boolean sispersAlgorithm(String inputString) {
        int n = inputString.length();
        ArrayList<String>[][] table = new ArrayList[n + 1][n + 1];

        //initializing table
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                table[i][j] = new ArrayList<>();
            }
        }


        // 1. For w = e, if S -> e is a rule, accept; else, reject. [ w = e case ]
        if (n == 0) {
            ArrayList<String> startRules = getRulesForVariable(startVariable);
            return startRules.contains("e");
        }


        // 2. For i = 1 to n: [ examine each substring of length 1 ]
        for (int i = 1; i <= n; i++) {
            // 3. For each variable A:
            String symbol = String.valueOf(inputString.charAt(i - 1));
            for (String A : variables) {
                // 4. Test whether A -> b is a rule
                ArrayList<String> varRules = getRulesForVariable(A);
                if (varRules.contains(symbol) && !table[i][i].contains(A)) {
                    // 5. If so, place A in table(i, i)
                    table[i][i].add(A);
                }
            }
        }

        // 6. For l = 2 to n: [l is the length of the substring ]
        for (int l = 2; l <= n; l++) {
            // 7. For i = 1 to n − l + 1: [ i is the start position of the substring ]
            for (int i = 1; i <= n - l + 1; i++) {
                // 8. Let j = i + l − 1. [ j is the end position of the substring ]
                int j = i + l - 1;
                // 9. For k = i to j − 1: [ k is the split position ]
                for (int k = i; k < j; k++) {
                    // 10. For each rule A -> BC:
                    for (String A : variables) {
                        ArrayList<String> aRules = getRulesForVariable(A);
                        for (String production : aRules) {
                            // 11. If table(i, k) contains B and table(k + 1, j) contains C, put A in table(i, j)
                            if (production.length() == 2 && variables.contains(String.valueOf(production.charAt(0)))
                                    && variables.contains(String.valueOf(production.charAt(1)))) {
                                String B = String.valueOf(production.charAt(0));
                                String C = String.valueOf(production.charAt(1));
                                if (table[i][k].contains(B) && table[k + 1][j].contains(C) && !table[i][j].contains(A)) {
                                    table[i][j].add(A);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 12. If S is in table(1, n), accept; else, rject
        return table[1][n].contains(startVariable);
    }

    /**
     * tests input strings with the grammar.
     *
     * @param inputFile file containing input strings.
     * @throws IOException if file readng fails.
     */
    static void testInputStrings(String inputFile) throws IOException {
        Scanner scanner = new Scanner(new File(inputFile));
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) input = "e"; // empty line = epsilon
            boolean accepted = sispersAlgorithm(input);
            System.out.println(input + ": " + (accepted ? "Accept" : "Reject"));
        }
        scanner.close();
    }
}

