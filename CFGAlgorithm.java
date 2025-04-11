import java.io.File;
import java.io.IOException;
import java.util.*;

public class CFGAlgorithm {

    static ArrayList<String> variables = new ArrayList<>(); // Non-terminal variables.
    static ArrayList<String> terminals = new ArrayList<>(); // Terminal symbols.
    static ArrayList<ArrayList<String>> rules = new ArrayList<>(); // Production rules.
    static ArrayList<String> ruleVariables = new ArrayList<>(); // LHS variable for each rule.
    static String startVariable = ""; // Start variable set to empty cause unknown at start.

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: java CFGTest <grammarFile> <inputFile>");
            return;
        }

        parseGrammar(args[0]);
        printGrammar();
        testInputStrings(args[1]);
    }

    /**
     * Parses the grammar from the given file.
     * Identifies variables, terminals, rules, and the start variable.
     *
     * @param grammarFile File containing the grammar.
     * @throws IOException If file reading fails.
     */
    //https://lazeecoder.medium.com/event-driven-context-free-grammar-cfg-parsing-66298c0f1ef7  gave us idea for LHS and RHS
    static void parseGrammar(String grammarFile) throws IOException {
        Scanner scanner = new Scanner(new File(grammarFile));
        int lineNumber = 1;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (lineNumber == 1) {
                variables.addAll(Arrays.asList(line.split(","))); // Get variables.
            } else if (lineNumber == 2) {
                terminals.addAll(Arrays.asList(line.split(","))); // Get terminals.
            } else if (!line.contains("->")) {
                startVariable = line; // Identify start variable.
            } else {
                String[] ruleSplit = line.split("->");
                String lhs = ruleSplit[0].trim();
                String[] rhsComponents = ruleSplit[1].trim().split("\\|");

                int lhsIndex = variables.indexOf(lhs);
                if (lhsIndex == -1) {
                    variables.add(lhs);
                    lhsIndex = variables.size() - 1;
                }

                for (String rhs : rhsComponents) {
                    ArrayList<String> production = new ArrayList<>(Arrays.asList(rhs.trim().split("\\s+"))); // Split by spaces
                    rules.add(production);
                    ruleVariables.add(lhs); // Map LHS variable to the rule.
                }
            }
            lineNumber++;
        }
        scanner.close();
    }

    /**
     * Prints the parsed grammar to ensure that the grammar is being interpretted correctly.
     */
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

    /**
     * Gets all rules for a given variable.
     *
     * @param variable The variable.
     * @return List of rules.
     */
    static ArrayList<String> getRulesForVariable(String variable) {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            if (ruleVariables.get(i).equals(variable)) {
                result.addAll(rules.get(i));
            }
        }
        return result;
    }

    /**
     * Implements Sipser's CYK algorithm to check string membership.
     *
     * @param inputString String to check.
     * @return True if the string is in the language, false otherwise.
     */
    static boolean sispersAlgorithm(String inputString) {
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

    /**
     * Tests input strings against the grammar.
     *
     * @param inputFile File containing input strings.
     * @throws IOException If file reading fails.
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

