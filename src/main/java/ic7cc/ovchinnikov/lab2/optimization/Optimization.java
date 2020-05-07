package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.logging.Logger;

public class Optimization {

    public static Grammar leftRecursionElimination(Grammar grammar) {
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production.getLhs(), production.getRhs().toArray(Symbol[]::new));
        }

        List<NonTerminal> nonTerms = new ArrayList<>(grammar.getNonTerminals());

        for (int i = 0, size = nonTerms.size(); i < size; i++) {
            for (int j = 0; j < i; j++) {
                Set<Production> productionsNonTerminalI = grammar.findProductionsByLhs(Symbol.of(nonTerms.get(i)));
                for (Production pi : productionsNonTerminalI) {
                    if (pi.getRhs().get(0).equals(Symbol.of(nonTerms.get(j)))) {
                        List<Symbol> rhspi = new LinkedList<>(pi.getRhs());
                        grammar.removeProduction(pi);
                        rhspi.remove(0);
                        Set<Production> productionsNonTerminalJ = grammar.findProductionsByLhs(Symbol.of(nonTerms.get(j)));
                        for (Production pj : productionsNonTerminalJ) {
                            List<Symbol> rhs = new LinkedList<>(pj.getRhs());
                            rhs.addAll(rhspi);
                            Production newProduction = new Production(nonTerms.get(i), rhs);
                            grammar.addProduction(newProduction);
                        }
                    }
                }
            }

            if (isRecursive(grammar, Symbol.of(nonTerms.get(i)))) {
                Set<Production> productionsNonTerminalI = grammar.findProductionsByLhs(Symbol.of(nonTerms.get(i)));
                for (Production production : productionsNonTerminalI) {
                    grammar.removeProduction(production);
                    if (production.getRhs().get(0).equals(Symbol.of(nonTerms.get(i)))) {
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.removeFirst();
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        grammar.addNonTerminals(newNonTerminal);
                        rhs.add(Symbol.of(newNonTerminal));
                        Production updateOldProduction = new Production(newNonTerminal, rhs);

                        Terminal eps = Terminal.EPSILON;
                        grammar.addTerminals(eps);
                        Production newProduction = new Production(newNonTerminal, new LinkedList<>() {{
                            add(Symbol.of(eps));
                        }});

                        grammar.addProduction(updateOldProduction);
                        grammar.addProduction(newProduction);
                    } else {
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        grammar.addNonTerminals(newNonTerminal);
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.remove(Symbol.of(Terminal.EPSILON));
                        rhs.add(Symbol.of(newNonTerminal));
                        Production updateOldProduction = new Production(production.getLhs(), rhs);

                        grammar.addProduction(updateOldProduction);
                    }
                }
            }
        }

        return grammar;
    }

    private static boolean isRecursive(Grammar grammar, Symbol nonTerm) {
        Set<Production> productions = grammar.findProductionsByLhs(nonTerm);
        for (Production p : productions) {
            if (p.getRhs().contains(Symbol.of(p.getLhs())))
                return true;
        }
        return false;
    }

    private static boolean isProductionLhsNonTerminalNextRhsNonTerminal(List<Production> productions, NonTerminal lhsNonTerminal, Symbol rhsNonTerminal) {
        for (Production production : productions) {
            if (production.getLhs().equals(lhsNonTerminal) && production.getRhs().contains(rhsNonTerminal))
                return true;
        }
        return false;
    }

    public static Grammar leftFactorization(Grammar grammar) {

        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));

        boolean isDont = true;
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            if (findAllPrefixOfNonTerminal(grammar, nonTerminal).size() > 0) {
                isDont = false;
            }
        }
        if (isDont)
            return grammar;

        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            Map<List<Symbol>, Set<Production>> map = findAllPrefixOfNonTerminal(grammar, nonTerminal);
            List<Symbol> symbols = null;
            int length = 0;
            for (Map.Entry<List<Symbol>, Set<Production>> entry : map.entrySet()) {
                if (entry.getKey().size() > length) {
                    length = entry.getKey().size();
                    symbols = entry.getKey();
                }
            }

            if (symbols != null) {
                Set<Production> productions = map.get(symbols);
                int i = 0;
                NonTerminal newNonTerminal = null;
                for (Production production : grammar.getProductions()) {
                    if (!productions.contains(production)) {
                        newGrammar.addProduction(production);
                    } else {
                        Iterator<Symbol> iteratorProdRhs = production.getRhs().iterator();
                        Iterator<Symbol> iteratorPrefix = symbols.iterator();
                        List<Symbol> newRhs = new LinkedList<>();
                        if (i == 0) {
                            i++;
                            newNonTerminal = new NonTerminal(production.getLhs().getName() + "'");
                            while (grammar.getNonTerminals().contains(newNonTerminal)) {
                                newNonTerminal = new NonTerminal(newNonTerminal.getName() + "'");
                            }
                        }
                        while (iteratorPrefix.hasNext()) {
                            if (iteratorProdRhs.hasNext()) {
                                Symbol symbolPrefix = iteratorPrefix.next();
                                Symbol symbolProdRhs = iteratorProdRhs.next();
                                if (symbolPrefix.equals(symbolProdRhs))
                                    newRhs.add(symbolPrefix);
                                else
                                    break;
                            }
                        }
                        List<Symbol> rhsNewProduction = new LinkedList<>();
                        while (iteratorProdRhs.hasNext()) {
                            rhsNewProduction.add(iteratorProdRhs.next());
                        }
                        newGrammar.addNonTerminals(newNonTerminal);
                        newGrammar.addProduction(newNonTerminal, rhsNewProduction.toArray(Symbol[]::new));
                        newRhs.add(Symbol.of(newNonTerminal));
                        newGrammar.addProduction(production.getLhs(), newRhs.toArray(Symbol[]::new));
                    }
                }
            }
        }

        return leftFactorization(newGrammar);
    }

    public static Map<List<Symbol>, Set<Production>> findAllPrefixOfNonTerminal(Grammar grammar, NonTerminal nonTerminal) {

        Map<List<Symbol>, Integer> allPrefixCount = new HashMap<>();
        Map<List<Symbol>, Set<Production>> allPrefixProduction = new HashMap<>();
        for (Production production : grammar.getProductions()) {
            if (production.getLhs().equals(nonTerminal)) {
                List<Symbol> prefix = new LinkedList<>();
                for (Symbol symbol : production.getRhs()) {
                    prefix.add(symbol);
                    int i = allPrefixCount.getOrDefault(prefix, 0);
                    allPrefixCount.put(new LinkedList<>(prefix), ++i);
                    Set<Production> productions = allPrefixProduction.getOrDefault(prefix, new HashSet<>());
                    productions.add(production);
                    allPrefixProduction.put(new LinkedList<>(prefix), productions);
                }
            }
        }
        Map<List<Symbol>, Set<Production>> resultPrefix = new HashMap<>();

        for (Map.Entry<List<Symbol>, Integer> entry : allPrefixCount.entrySet()) {
            if (entry.getValue() > 1) {
                resultPrefix.put(entry.getKey(), allPrefixProduction.get(entry.getKey()));
            }
        }

        return resultPrefix;
    }

    public static Grammar removeLongRules(Grammar grammar) {
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production);
            List<Symbol> rhs = production.getRhs();
            if (rhs.size() > 2) {
                NonTerminal nonTerminal = production.getLhs();
                for (int i = 0; i < rhs.size() - 2; i++) {
                    NonTerminal newNonTerminal = new NonTerminal(production.getLhs().getName() + i);
                    newGrammar.addNonTerminals(newNonTerminal);
                    List<Symbol> newRhs = new ArrayList<>();

                    newRhs.add(rhs.get(i));
                    newRhs.add(Symbol.of(newNonTerminal));
                    Production newProduction = new Production(nonTerminal, newRhs);
                    nonTerminal = newNonTerminal;
                    newGrammar.addProduction(newProduction);
                }
                Production prod = new Production(nonTerminal, new LinkedList<>() {{
                    add(rhs.get(rhs.size() - 2));
                    add(rhs.get(rhs.size() - 1));
                }});
                newGrammar.addProduction(prod);

                newGrammar.removeProduction(production);
            }
        }
        return newGrammar;
    }

    public static Grammar removeEpsilonRules(Grammar grammar) {
        Map<Integer, Production> productionMap = new HashMap<>();
        int i = 0;
        for (Production production : grammar.getProductions()) {
            if (!isContainsOnlyTerminal(production))
                productionMap.put(++i, production);
        }

        Map<NonTerminal, Set<Integer>> concernedRules = new HashMap<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            Set<Integer> numberRules = new HashSet<>();
            for (Map.Entry<Integer, Production> entry : productionMap.entrySet()) {
                if (entry.getValue().getRhs().contains(Symbol.of(nonTerminal)))
                    numberRules.add(entry.getKey());
            }
            concernedRules.put(nonTerminal, numberRules);
        }

        Map<NonTerminal, Boolean> isEpsilon = new HashMap<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            isEpsilon.put(nonTerminal, false);
        }

        Map<Integer, Integer> counter = new HashMap<>();
        Queue<NonTerminal> queue = new ArrayDeque<>();
        for (Map.Entry<Integer, Production> entry : productionMap.entrySet()) {
            int count = 0;
            for (Symbol symbol : entry.getValue().getRhs())
                if (symbol.isTerminal() || symbol.isNonTerminal())
                    count++;
            counter.put(entry.getKey(), count);
            if (count == 0) {
                queue.add(entry.getValue().getLhs());
                isEpsilon.put(entry.getValue().getLhs(), true);
            }
        }

        while (!queue.isEmpty()) {
            NonTerminal nonTerminal = queue.remove();
            Set<Integer> numberProd = concernedRules.get(nonTerminal);
            for (int number : numberProd) {
                counter.put(number, counter.get(number) - 1);
                if (counter.get(number) == 0) {
                    NonTerminal nt = productionMap.get(number).getLhs();
                    if (!isEpsilon.get(nt)) {
                        queue.add(nt);
                        isEpsilon.put(nt, true);
                    }
                }
            }
        }

        Grammar newGrammar;
        if (isEpsilon.get(grammar.getStartSymbol())) {
            newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName() + "'");
            NonTerminal newTerminalSp = newGrammar.getStartSymbol();
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addNonTerminals(newTerminalSp);
            newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
            newGrammar.addProduction(newTerminalSp, Symbol.of(grammar.getStartSymbol()));
            newGrammar.addProduction(newTerminalSp, Symbol.of(Terminal.EPSILON));
        } else {
            newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        }

        for (Production production : grammar.getProductions()) {
            List<Symbol> rhs = production.getRhs();
            if (!rhs.contains(Symbol.of(Terminal.EPSILON))) {
                if (rhs.size() == 2 && rhs.get(0).isNonTerminal() && rhs.get(1).isNonTerminal() &&
                        isEpsilon.get(rhs.get(0).isNonTerminalGetting()) && isEpsilon.get(rhs.get(1).isNonTerminalGetting())) {
                    newGrammar.addProduction(production.getLhs(), rhs.get(0));
                    newGrammar.addProduction(production.getLhs(), rhs.get(1));
                } else if (rhs.size() == 2 && rhs.get(0).isNonTerminal() && isEpsilon.get(rhs.get(0).isNonTerminalGetting())) {
                    newGrammar.addProduction(production.getLhs(), rhs.get(1));
                } else if (rhs.size() == 2 && rhs.get(1).isNonTerminal() && isEpsilon.get(rhs.get(1).isNonTerminalGetting())) {
                    newGrammar.addProduction(production.getLhs(), rhs.get(0));
                }
                newGrammar.addProduction(production.getLhs(), rhs.toArray(Symbol[]::new));
            }

        }

        return newGrammar;
    }

    public static boolean isContainsOnlyTerminal(Production production) {
        for (Symbol symbol : production.getRhs()) {
            if (symbol.isNonTerminal() || symbol.isEpsilon())
                return false;
        }
        return true;
    }

    public static Grammar removeChainRules(Grammar grammar) {
        Queue<Pair<NonTerminal, NonTerminal>> queue = new ArrayDeque<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            queue.add(new Pair<>(nonTerminal, nonTerminal));
        }

        Set<Pair<NonTerminal, NonTerminal>> set = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            Pair<NonTerminal, NonTerminal> pair = queue.remove();
            set.add(pair);
            for (Production production : grammar.getProductions()) {
                if (production.getRhs().size() == 1 && production.getRhs().get(0).isNonTerminal() && pair.second.equals(production.getLhs())) {
                    queue.add(new Pair<>(pair.first, production.getRhs().get(0).isNonTerminalGetting()));
                }
            }
        }

        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        for (Pair<NonTerminal, NonTerminal> pair : set) {
            for (Production production : grammar.getProductions()) {
                if (!pair.second.equals(pair.first)) {
                    if (!isChainRule(production) && production.getLhs().equals(pair.second)) {
                        newGrammar.addProduction(pair.first, production.getRhs().toArray(Symbol[]::new));
                    }
                } else if (!isChainRule(production)) {
                    newGrammar.addProduction(production);
                }
            }
        }

        return newGrammar;
    }

    private static boolean isChainRule(Production production) {
        return production.getRhs().size() == 1 && production.getRhs().get(0).isNonTerminal();
    }

    public static Grammar removeUselessCharacter(Grammar grammar) {
        Grammar updateGrammar = removeNonGeneratingNonTerminals(grammar);
        return removeUnreachableNonTerminal(updateGrammar);
    }

    public static Grammar removeNonGeneratingNonTerminals(Grammar grammar) {
        Map<Integer, Production> productionMap = new HashMap<>();
        int i = 0;
        for (Production production : grammar.getProductions()) {
            productionMap.put(++i, production);
        }

        Map<NonTerminal, Set<Integer>> concernedRules = new HashMap<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            Set<Integer> numberRules = new HashSet<>();
            for (Map.Entry<Integer, Production> entry : productionMap.entrySet()) {
                if (entry.getValue().getRhs().contains(Symbol.of(nonTerminal)))
                    numberRules.add(entry.getKey());
            }
            concernedRules.put(nonTerminal, numberRules);
        }

        Map<NonTerminal, Boolean> isGenerating = new HashMap<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            isGenerating.put(nonTerminal, false);
        }

        Map<Integer, Integer> counter = new HashMap<>();
        Queue<NonTerminal> queue = new ArrayDeque<>();
        for (Map.Entry<Integer, Production> entry : productionMap.entrySet()) {
            Set<Symbol> terminals = new HashSet<>();
            for (Symbol symbol : entry.getValue().getRhs())
                if (symbol.isNonTerminal())
                    terminals.add(symbol);
            counter.put(entry.getKey(), terminals.size());
            if (terminals.size() == 0) {
                queue.add(entry.getValue().getLhs());
                isGenerating.put(entry.getValue().getLhs(), true);
            }
        }

        while (!queue.isEmpty()) {
            NonTerminal nonTerminal = queue.remove();
            Set<Integer> numberProd = concernedRules.get(nonTerminal);
            for (int number : numberProd) {
                counter.put(number, counter.get(number) - 1);
                if (counter.get(number) == 0) {
                    NonTerminal nt = productionMap.get(number).getLhs();
                    if (!isGenerating.get(nt)) {
                        queue.add(nt);
                        isGenerating.put(nt, true);
                    }
                }
            }
        }
        Set<Production> productions = new LinkedHashSet<>(grammar.getProductions());
        for (Production production : grammar.getProductions()) {
            for (Symbol symbol : production.getRhs()) {
                if (symbol.isNonTerminal() && !isGenerating.get(symbol.isNonTerminalGetting())) {
                    productions.remove(production);
                    break;
                }
            }
        }
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        for (Production production : productions) {
            newGrammar.addProduction(production);
        }

        return newGrammar;
    }

    public static Grammar removeUnreachableNonTerminal(Grammar grammar) {

        Set<NonTerminal> nonTerminals = new HashSet<>();
        nonTerminals.add(grammar.getStartSymbol());
        Queue<NonTerminal> queue = new ArrayDeque<>();
        queue.add(grammar.getStartSymbol());
        while (!queue.isEmpty()) {
            NonTerminal nonTerminal = queue.remove();
            for (Production production : grammar.getProductions()) {
                if (nonTerminal.equals(production.getLhs())) {
                    for (Symbol symbol : production.getRhs()) {
                        if (symbol.isNonTerminal() && !nonTerminals.contains(symbol.isNonTerminalGetting())) {
                            queue.add(symbol.isNonTerminalGetting());
                            nonTerminals.add(symbol.isNonTerminalGetting());
                        }
                    }
                }
            }
        }

        Set<Production> productions = new LinkedHashSet<>();
        for (Production production : grammar.getProductions()) {
            if (nonTerminals.contains(production.getLhs()))
                productions.add(production);
        }
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : productions) {
            newGrammar.addProduction(production);
        }

        return newGrammar;
    }

    public static Grammar removeMeetingSeveralTerminals(Grammar grammar) {

        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : grammar.getProductions()) {
            List<Symbol> rhs = production.getRhs();
            if (rhs.size() == 2 && (rhs.get(0).isTerminal() || rhs.get(1).isTerminal())) {
                List<Symbol> newRhs = new ArrayList<>();
                if (rhs.get(0).isTerminal()) {
                    NonTerminal nt = new NonTerminal(rhs.get(0).getName().toUpperCase() + "'");
                    newGrammar.addNonTerminals(nt);
                    List<Symbol> nrhs = new LinkedList<>();
                    nrhs.add(rhs.get(0));
                    Production np = new Production(nt, nrhs);
                    newGrammar.addProduction(np);
                    newRhs.add(Symbol.of(nt));
                } else {
                    newRhs.add(rhs.get(0));
                }

                if (rhs.get(1).isTerminal()) {
                    NonTerminal nt = new NonTerminal(rhs.get(1).getName().toUpperCase() + "'");
                    newGrammar.addNonTerminals(nt);
                    List<Symbol> nrhs = new LinkedList<>();
                    nrhs.add(rhs.get(1));
                    Production np = new Production(nt, nrhs);
                    newGrammar.addProduction(np);
                    newRhs.add(Symbol.of(nt));
                } else {
                    newRhs.add(rhs.get(1));
                }
                Production newProduction = new Production(production.getLhs(), newRhs);
                newGrammar.addProduction(newProduction);
            } else {
                newGrammar.addProduction(production);
            }
        }
        return newGrammar;
    }

    public static Grammar conversionToChomskyNormalForm(Grammar grammar) {
        boolean isRightStartSymbol = false;
        for (Production production : grammar.getProductions()) {
            if (production.getRhs().contains(Symbol.of(grammar.getStartSymbol()))) {
                isRightStartSymbol = true;
                break;
            }
        }
        if (isRightStartSymbol) {
            Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName() + "'");
            NonTerminal nonTerminal = newGrammar.getStartSymbol();
            List<NonTerminal> nonTerminals = new LinkedList<>(grammar.getNonTerminals());
            nonTerminals.add(nonTerminal);
            newGrammar.addNonTerminals(nonTerminals.toArray(NonTerminal[]::new));
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addProduction(nonTerminal, Symbol.of(grammar.getStartSymbol()));
            for (Production production : grammar.getProductions()) {
                newGrammar.addProduction(production);
            }
            grammar = newGrammar;
        }

        return removeDontUseNonTerminals(
                removeMeetingSeveralTerminals(
                        removeUselessCharacter(
                                removeChainRules(
                                        removeEpsilonRules(
                                                removeLongRules(grammar)
                                        )
                                )
                        )
                )
        );
    }

    private static Grammar removeDontUseNonTerminals(Grammar grammar) {
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));

        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            boolean isUse = false;
            for (Production production : grammar.getProductions()) {
                if (nonTerminal.equals(production.getLhs())) {
                    isUse = true;
                    break;
                }
            }
            if (isUse)
                newGrammar.addNonTerminals(nonTerminal);
        }
        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production);
        }
        return newGrammar;
    }

    private static class Pair<T, S> {
        private final T first;
        private final S second;

        public Pair(T first, S second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair<?, ?> pair = (Pair<?, ?>) o;
            return Objects.equals(first, pair.first) &&
                    Objects.equals(second, pair.second);
        }

        @Override
        public int hashCode() {
            return Objects.hash(first, second);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }
    }
}
