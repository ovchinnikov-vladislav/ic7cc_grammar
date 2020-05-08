package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;
import ic7cc.ovchinnikov.lab2.util.Pair;

import java.util.*;

public class ChomskyNormalGrammarBuilder {

    public static Grammar build(Grammar grammar) {
        boolean isRightStartSymbol = false;
        for (Production production : grammar.getProductions()) {
            if (production.getRhs().contains(Symbol.of(grammar.getStartSymbol()))) {
                isRightStartSymbol = true;
                break;
            }
        }
        if (isRightStartSymbol) {
            Grammar newGrammar = new Grammar(grammar.getName(), grammar.createNewNonTerminal(grammar.getStartSymbol().getName() + "'").getName());
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

    private static Grammar removeLongRules(Grammar grammar) {
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production);
            List<Symbol> rhs = production.getRhs();
            if (rhs.size() > 2) {
                NonTerminal nonTerminal = production.getLhs();
                for (int i = 0; i < rhs.size() - 2; i++) {
                    NonTerminal newNonTerminal = newGrammar.createNewNonTerminal(production.getLhs().getName() + i);
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
        System.out.println(newGrammar);
        return newGrammar;
    }

    private static Grammar removeEpsilonRules(Grammar grammar) {
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
            newGrammar = new Grammar(grammar.getName(), grammar.createNewNonTerminal(grammar.getStartSymbol().getName() + "'").getName());
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
        System.out.println(newGrammar);
        return newGrammar;
    }

    private static boolean isContainsOnlyTerminal(Production production) {
        for (Symbol symbol : production.getRhs()) {
            if (symbol.isNonTerminal() || symbol.isEpsilon())
                return false;
        }
        return true;
    }

    private static Grammar removeChainRules(Grammar grammar) {
        Queue<Pair<NonTerminal, NonTerminal>> queue = new ArrayDeque<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            queue.add(new Pair<>(nonTerminal, nonTerminal));
        }

        Set<Pair<NonTerminal, NonTerminal>> set = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            Pair<NonTerminal, NonTerminal> pair = queue.remove();
            set.add(pair);
            for (Production production : grammar.getProductions()) {
                if (production.getRhs().size() == 1 && production.getRhs().get(0).isNonTerminal() && pair.s().equals(production.getLhs())) {
                    queue.add(new Pair<>(pair.f(), production.getRhs().get(0).isNonTerminalGetting()));
                }
            }
        }

        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        for (Pair<NonTerminal, NonTerminal> pair : set) {
            for (Production production : grammar.getProductions()) {
                if (!pair.s().equals(pair.f())) {
                    if (!isChainRule(production) && production.getLhs().equals(pair.s())) {
                        newGrammar.addProduction(pair.f(), production.getRhs().toArray(Symbol[]::new));
                    }
                } else if (!isChainRule(production)) {
                    newGrammar.addProduction(production);
                }
            }
        }
        System.out.println(newGrammar);
        return newGrammar;
    }

    private static boolean isChainRule(Production production) {
        return production.getRhs().size() == 1 && production.getRhs().get(0).isNonTerminal();
    }

    private static Grammar removeUselessCharacter(Grammar grammar) {
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
        System.out.println(newGrammar);
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
        System.out.println(newGrammar);
        return newGrammar;
    }

    // Удаление тех правил, в которых встречаются несколько терминалов, и замена их новыми.
    // Работает только для правил, в которых в правой части содержатся 2 символа (хотя бы один символ терминал)
    public static Grammar removeMeetingSeveralTerminals(Grammar grammar) {

        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        Map<Terminal, NonTerminal> helpMapTerminals = new HashMap<>();

        for (Production production : grammar.getProductions()) {
            List<Symbol> rhs = production.getRhs();
            if (rhs.size() == 2 && (rhs.get(0).isTerminal() || rhs.get(1).isTerminal())) {
                List<Symbol> newRhs = new ArrayList<>();
                if (rhs.get(0).isTerminal()) {
                    NonTerminal nt = helpMapTerminals.getOrDefault(rhs.get(0).isTerminalGetting(),
                            newGrammar.createNewNonTerminal(rhs.get(0).getName().toUpperCase() + "'"));
                    helpMapTerminals.put(rhs.get(0).isTerminalGetting(), nt);
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
                    NonTerminal nt = helpMapTerminals.getOrDefault(rhs.get(1).isTerminalGetting(),
                            newGrammar.createNewNonTerminal(rhs.get(1).getName().toUpperCase() + "'"));
                    helpMapTerminals.put(rhs.get(1).isTerminalGetting(), nt);
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
        System.out.println(newGrammar);
        return newGrammar;
    }

    // Удаление неиспользуемых символов
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

}
