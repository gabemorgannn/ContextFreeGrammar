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