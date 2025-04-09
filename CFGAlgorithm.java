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

import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * Represents a single grammar production rule (LHS -> RHS).
 * Includes logic to determine the rule type based on parsed symbols.
 */
class Rule {
    final String lhs;         // Left-hand side variable
    final List<String> rhs; // Right-hand side production (list of symbols)
    final RuleType type;    // Type of the rule (TERMINAL, VARIABLE, EPSILON, OTHER)

    enum RuleType { TERMINAL, VARIABLE, EPSILON, OTHER }

    /**
     * Constructor for Rule.
     * Determines rule type based on RHS content and known variable/terminal sets.
     * @param lhs The left-hand side variable.
     * @param rhs The list of symbols on the right-hand side.
     * @param variables A set of all known variable names in the grammar.
     * @param terminals A set of all known terminal names in the grammar.
     */
    Rule(String lhs, List<String> rhs, Set<String> variables, Set<String> terminals) {
        this.lhs = lhs;
        this.rhs = Collections.unmodifiableList(new ArrayList<>(rhs)); // Make immutable copy

        // Determine Rule Type
        if (rhs.size() == 1) {
            String symbol = rhs.get(0);
            if (symbol.equals("e")) {
                this.type = RuleType.EPSILON;
            } else if (terminals.contains(symbol)) {
                this.type = RuleType.TERMINAL; // A -> a
            } else {
                // Could be A -> B (unit rule - not strictly CNF) or malformed
                this.type = RuleType.OTHER;
            }
        } else if (rhs.size() == 2) {
            String symbol1 = rhs.get(0);
            String symbol2 = rhs.get(1);
            // Check if both RHS symbols are known variables
            if (variables.contains(symbol1) && variables.contains(symbol2)) {
                this.type = RuleType.VARIABLE; // A -> BC
            } else {
                this.type = RuleType.OTHER; // Malformed CNF rule
            }
        } else {
            // Rules not in standard CNF structure (A->a, A->BC, S->e)
            this.type = RuleType.OTHER;
        }
    }

    @Override
    public String toString() {
       // Format for printing within the grammar summary
       return lhs + " -> " + String.join(" ", rhs);
    }
}


/**
 * Main class for the CFG Parser project.
 * Parses a grammar file, implements the CYK algorithm (based on Sipser),
 * and tests input strings for membership. Avoids using Map for rule storage.
 */
public class CFGAlgorithm {

    // --- Grammar Representation ---
    private Set<String> variables;
    private Set<String> terminals;
    private List<Rule> allRules; // Store all rules in a single list
    private Set<String> epsilonRules; // Variables A such that A -> e
    private String startVariable;

    // Constructor initializes empty grammar structures
    public CFGAlgorithm() {
        variables = new HashSet<>();
        terminals = new HashSet<>();
        allRules = new ArrayList<>();
        epsilonRules = new HashSet<>();
        startVariable = null;
    }

    // --- Grammar Parsing Logic ---

    /**
     * Parses the grammar definition file.
     * @param filepath Path to the grammar file.
     * @throws IOException If file reading fails.
     * @throws IllegalArgumentException If grammar format is invalid.
     */
    public void parseGrammar(String filepath) throws IOException, IllegalArgumentException {
        List<String> lines = Files.lines(Paths.get(filepath))
                                  .map(String::trim)
                                  .filter(line -> !line.isEmpty())
                                  .collect(Collectors.toList());

        if (lines.size() < 4) {
            throw new IllegalArgumentException("Error: Grammar file '" + filepath + "' has too few lines.");
        }

        // Line 1: Variables
        variables.addAll(Arrays.asList(lines.get(0).split("\\s*,\\s*")));
        if (variables.isEmpty()) {
             throw new IllegalArgumentException("Error: No variables defined in grammar file.");
        }

        // Line 2: Terminals
        terminals.addAll(Arrays.asList(lines.get(1).split("\\s*,\\s*")));
        if (terminals.isEmpty()) {
             throw new IllegalArgumentException("Error: No terminals defined in grammar file.");
        }
        // Ensure 'e' isn't treated as a standard terminal if present
        terminals.remove("e");


        // Last Line: Start Variable
        startVariable = lines.get(lines.size() - 1).trim();
        if (!variables.contains(startVariable)) {
             throw new IllegalArgumentException("Error: Start variable '" + startVariable + "' not listed in declared variables.");
        }

        // --- Robust RHS Parsing ---
        RhsParser rhsParser = new RhsParser(variables, terminals);

        // Middle Lines: Rules
        for (int i = 2; i < lines.size() - 1; i++) {
            String line = lines.get(i);
            if (!line.contains("->")) {
                System.err.println("Warning: Skipping malformed rule line (no '->'): " + line);
                continue;
            }
            String[] parts = line.split("\\s*->\\s*", 2);
            if (parts.length != 2) {
                 System.err.println("Warning: Skipping malformed rule line (format error): " + line);
                 continue;
            }
            String lhs = parts[0].trim();
            if (!variables.contains(lhs)) {
                 System.err.println("Warning: LHS variable '" + lhs + "' in rule '" + line + "' not declared. Skipping.");
                 continue;
            }

            String[] rhsProductions = parts[1].split("\\s*\\|\\s*");

            for (String prodStr : rhsProductions) {
                prodStr = prodStr.trim();
                if (prodStr.isEmpty()){
                    System.err.println("Warning: Skipping empty production for LHS '" + lhs + "' in rule line: " + line);
                    continue;
                }

                List<String> productionSymbols = rhsParser.parse(prodStr);

                if (productionSymbols.isEmpty() && !prodStr.equals("e")){
                    System.err.println("Warning: Could not parse RHS '" + prodStr + "' for LHS '" + lhs + "'. Skipping rule.");
                    continue;
                }
                 if (prodStr.equals("e")) {
                     productionSymbols = Collections.singletonList("e");
                 }

                 Rule rule = new Rule(lhs, productionSymbols, variables, terminals);
                 allRules.add(rule);

                 if (rule.type == Rule.RuleType.EPSILON) {
                    epsilonRules.add(lhs);
                 }
            }
        }
    }

    /** Inner helper class to parse RHS strings based on known variables/terminals. */
    private static class RhsParser {
        private final List<String> sortedSymbols; // Known vars/terms sorted by length desc

        RhsParser(Set<String> variables, Set<String> terminals) {
            this.sortedSymbols = new ArrayList<>(variables);
            this.sortedSymbols.addAll(terminals);
            this.sortedSymbols.sort((s1, s2) -> Integer.compare(s2.length(), s1.length()));
        }

        List<String> parse(String rhs) {
            List<String> symbols = new ArrayList<>();
            if (rhs.equals("e")) return symbols; // Return empty for epsilon case

            int currentPos = 0;
            while (currentPos < rhs.length()) {
                boolean matchFound = false;
                for (String symbol : sortedSymbols) {
                    if (rhs.startsWith(symbol, currentPos)) {
                        symbols.add(symbol);
                        currentPos += symbol.length();
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    System.err.println("Error parsing RHS '" + rhs + "' at position " + currentPos + ". Unrecognized symbol sequence.");
                    return Collections.emptyList(); // Indicate failure
                }
            }
            return symbols;
        }
    }


    // --- CYK Algorithm Implementation ---
    /** Checks membership using CYK algorithm. */
    public boolean cykAlgorithm(String inputString) {
        // Split input string into array of single characters (terminals)
        String[] w = inputString.chars().mapToObj(c -> String.valueOf((char)c)).toArray(String[]::new);
        int n = w.length;

        // Step 1: Handle empty string case
        if (n == 0) {
            return epsilonRules.contains(startVariable);
        }

        // Initialize DP table
        Set<String>[][] table = new HashSet[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                table[i][j] = new HashSet<>();
            }
        }

        // Step 2-5: Base case (length 1)
        for (int i = 0; i < n; i++) {
            String terminal = w[i];
            for (Rule rule : allRules) {
                if (rule.type == Rule.RuleType.TERMINAL && rule.rhs.get(0).equals(terminal)) {
                    table[i][i].add(rule.lhs);
                }
            }
        }

        // Step 6-11: Substrings length > 1
        for (int len = 2; len <= n; len++) {
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                for (int k = i; k < j; k++) {
                    for (Rule rule : allRules) {
                        if (rule.type == Rule.RuleType.VARIABLE) { // A -> BC rule
                            // Added size check for robustness, though type check implies size 2
                            if (rule.rhs.size() == 2) {
                                String varB = rule.rhs.get(0);
                                String varC = rule.rhs.get(1);
                                if (table[i][k].contains(varB) && table[k + 1][j].contains(varC)) {
                                    table[i][j].add(rule.lhs);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 12: Final check
        return table[0][n - 1].contains(startVariable);
    }

    // --- Printing Grammar Details ---
     @Override
     public String toString() {
        StringBuilder sb = new StringBuilder();
        List<String> sortedVars = new ArrayList<>(variables);
        Collections.sort(sortedVars);
        sb.append("Variables: ").append(String.join(", ", sortedVars)).append("\n");

        // Determine terminals to print (including 'e' if used)
        Set<String> termsToPrint = new HashSet<>(terminals);
         if (!epsilonRules.isEmpty() || allRules.stream().anyMatch(r -> r.type == Rule.RuleType.EPSILON)) {
             termsToPrint.add("e");
         }
        List<String> sortedTerms = new ArrayList<>(termsToPrint);
        Collections.sort(sortedTerms);
        sb.append("Terminals: ").append(String.join(", ", sortedTerms)).append("\n");

        sb.append("Rules:\n");
        // Sort rules for consistent output
        allRules.sort(Comparator.comparing((Rule r) -> r.lhs)
                               .thenComparing(r -> String.join("", r.rhs))); // Sort key uses no space
        for (Rule rule : allRules) {
            // Use rule's toString which now includes spaces in RHS
            sb.append("    ").append(rule).append("\n");
        }

        sb.append("Start Variable: ").append(startVariable);
        return sb.toString();
     }


    // --- Main Execution Logic ---
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java CFGAlgorithm <grammar_file> <input_file>");
            System.exit(1);
        }

        String grammarFile = args[0];
        String inputFile = args[1];

        CFGAlgorithm parser = new CFGAlgorithm();

        try {
            // Parse Grammar
            parser.parseGrammar(grammarFile);

            // Print Grammar Details (as required by project)
            System.out.println(parser);
            System.out.println(); // Blank line separating grammar from results

            // Process Input Strings
             List<String> inputStrings = Files.readAllLines(Paths.get(inputFile));

             for (String inputString : inputStrings) {
                 if (inputString == null) continue;

                 String trimmedInput = inputString.trim();
                 boolean accepts = parser.cykAlgorithm(trimmedInput);
                 String result = accepts ? "Accept" : "Reject";

                 // Output format as per project example
                 System.out.println(inputString + ": " + result);
            }

        } catch (IOException e) {
            System.err.println("ERROR: Could not read file: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
             System.err.println("ERROR: Invalid grammar file: " + e.getMessage());
             System.exit(1);
        } catch (Exception e) {
             System.err.println("An unexpected error occurred: " + e.getMessage());
             e.printStackTrace();
             System.exit(1);
        }
    }
}

// # Test the original astar grammar
// java CFGParser astarGrammar.txt astarGrammarInput.txt

// # Test the anbn grammar
// java CFGParser anbnGrammar.txt anbnGrammarInput.txt

// # Test the palindrome grammar
// java CFGParser palindromeGrammar.txt palindromeGrammarInput.txt

// # Test the equalAsBs grammar
// java CFGParser equalAsBsGrammar.txt equalAsBsGrammarInput.txt