// PROOF The following algorithm D implements the proof idea. Let G be
// a CFG in Chomsky normal form generating the CFL L. Assume that S is the
// start variable. (Recall that the empty string is handled specially in a Chomsky
// normal form grammar. The algorithm handles the special case in which w = ε
// in stage 1.) Comments appear inside double brackets.
// D = “On input w = w1 · · · wn:
// 1. For w = ε, if S → ε is a rule, accept; else, reject. [[ w = ε case ]]
// 2. For i = 1 to n: [[ examine each substring of length 1 ]]
// 3. For each variable A:
// 4. Test whether A → b is a rule, where b = wi
// .
// 5. If so, place A in table(i, i).
// 6. For l = 2 to n: [[ l is the length of the substring ]]
// 7. For i = 1 to n − l + 1: [[ i is the start position of the substring ]]
// 8. Let j = i + l − 1. [[ j is the end position of the substring ]]
// 9. For k = i to j − 1: [[ k is the split position ]]
// 10. For each rule A → BC:
// 11. If table(i, k) contains B and table(k + 1, j) contains C, put A in table(i, j).
// 12. If S is in table(1, n), accept; else, reject.”

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CFGAlgorithm {

    static ArrayList<String> variables = new ArrayList<>();
    static ArrayList<String> terminals = new ArrayList<>();
    static ArrayList<ArrayList<String>> rules = new ArrayList<>();
    static ArrayList<String> ruleVariables = new ArrayList<>(); // Stores the LHS variable for each rule
    static String startVariable = "";

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java CFGTest <grammarFile> <inputFile>");
            return;
        }

        parseGrammar(args[0]);
        printGrammar();
        testInputStrings(args[1]);
    }

    static void parseGrammar(String grammarFile) throws IOException {
        Scanner scanner = new Scanner(new File(grammarFile));
        int lineNumber = 1;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (lineNumber == 1) {
                variables.addAll(Arrays.asList(line.split(",\\s*")));
            } else if (lineNumber == 2) {
                terminals.addAll(Arrays.asList(line.split(",\\s*")));
            } else if (!line.contains("->")) {
                startVariable = line;
            } else {
                String[] parts = line.split("->");
                String lhs = parts[0].trim();
                String[] rhsParts = parts[1].trim().split("\\|");

                int lhsIndex = variables.indexOf(lhs);
                if (lhsIndex == -1) {
                    variables.add(lhs);
                    lhsIndex = variables.size() - 1;
                }

                for (String rhs : rhsParts) {
                    ArrayList<String> production = new ArrayList<>(Arrays.asList(rhs.trim().split("\\s+"))); // Split by spaces
                    rules.add(production);
                    ruleVariables.add(lhs);
                }
            }
            lineNumber++;
        }
        scanner.close();
    }

    static void printGrammar() {
        System.out.println("Variables: " + String.join(", ", variables));
        System.out.println("Terminals: " + String.join(", ", terminals));
        System.out.println("Rules:");
        for (int i = 0; i < rules.size(); i++) {
            System.out.println(ruleVariables.get(i) + " -> " + String.join(" | ", rules.get(i)));
        }
        System.out.println("Start Variable: " + startVariable);
        System.out.println();
    }

    static void testInputStrings(String inputFile) throws IOException {
        Scanner scanner = new Scanner(new File(inputFile));
        while (scanner.hasNextLine()) {
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) input = "e"; // empty line = epsilon
            boolean accepted = isStringInLanguage(input);
            System.out.println(input + ": " + (accepted ? "Accept" : "Reject"));
        }
        scanner.close();
    }

    static boolean isStringInLanguage(String inputString) {
        int n = inputString.length();
        ArrayList<String>[][] table = new ArrayList[n + 1][n + 1];

        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n; j++) {
                table[i][j] = new ArrayList<>();
            }
        }

        // 1. For w = ε, if S → ε is a rule, accept; else, reject. [[ w = ε case ]]
        if (n == 0) {
            ArrayList<String> startRules = getRulesForVariable(startVariable);
            return startRules.contains("e");
        }

        // 2. For i = 1 to n: [[ examine each substring of length 1 ]]
        for (int i = 1; i <= n; i++) {
            // 3. For each variable A:
            String symbol = String.valueOf(inputString.charAt(i - 1));
            for (String A : variables) {
                // 4. Test whether A → b is a rule, where b = wi .
                ArrayList<String> varRules = getRulesForVariable(A);
                if (varRules.contains(symbol) && !table[i][i].contains(A)) {
                    // 5. If so, place A in table(i, i).
                    table[i][i].add(A);
                }
            }
        }

        // 6. For l = 2 to n: [[ l is the length of the substring ]]
        for (int l = 2; l <= n; l++) {
            // 7. For i = 1 to n − l + 1: [[ i is the start position of the substring ]]
            for (int i = 1; i <= n - l + 1; i++) {
                // 8. Let j = i + l − 1. [[ j is the end position of the substring ]]
                int j = i + l - 1;
                // 9. For k = i to j − 1: [[ k is the split position ]]
                for (int k = i; k < j; k++) {
                    // 10. For each rule A → BC:
                    for (String A : variables) {
                        ArrayList<String> aRules = getRulesForVariable(A);
                        for (String production : aRules) {
                            // 11. If table(i, k) contains B and table(k + 1, j) contains C, put A in table(i, j).
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

        // 12. If S is in table(1, n), accept; else, reject.”
        return table[1][n].contains(startVariable);
    }

    static ArrayList<String> getRulesForVariable(String variable) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            if (ruleVariables.get(i).equals(variable)) {
                result.addAll(rules.get(i));
            }
        }
        return result;
    }
}

// # Test the original astar grammar
// java CFGAlgorithm astarGrammar.txt astarGrammarInput.txt

// # Test the anbn grammar
// java CFGAlgorithm anbnGrammar.txt anbnGrammarInput.txt

// # Test the palindrome grammar
// java CFGAlgorithm palindromeGrammar.txt palindromeGrammarInput.txt

// # Test the equalAsBs grammar
// java CFGAlgorithm equalAsBsGrammar.txt equalAsBsGrammarInput.txt

//https://lazeecoder.medium.com/event-driven-context-free-grammar-cfg-parsing-66298c0f1ef7 