package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;

import java.util.*;

public class Optimization {

    // TODO: Провести дебагинг и отладку. Вроде бы работает, написать тесты.
    public static Grammar leftRecursionElimination(Grammar grammar) {
        NonTerminal startTerminal = new NonTerminal(grammar.getStartSymbol().getName());
        Grammar newGrammar = new Grammar(grammar.getName(), startTerminal);
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production.getLhs(), production.getRhs().toArray(Symbol[]::new));
        }

        List<NonTerminal> nonTerms = new ArrayList<>(grammar.getNonTerminals());

        for (int i = 0, size = nonTerms.size(); i < size; i++) {
            for (int j = 0; j < i; j++) {
                Set<Production> productionsNonTerminalI = grammar.findProductionsByLhs(nonTerms.get(i));
                for (Production pi : productionsNonTerminalI) {
                    if (pi.getRhs().get(0).equals(nonTerms.get(j))) {
                        List<Symbol> rhspi = new LinkedList<>(pi.getRhs());
                        grammar.removeProduction(pi);
                        rhspi.remove(0);
                        Set<Production> productionsNonTerminalJ = grammar.findProductionsByLhs(nonTerms.get(j));
                        for (Production pj : productionsNonTerminalJ) {
                            List<Symbol> rhs = new LinkedList<>(pj.getRhs());
                            rhs.addAll(rhspi);
                            Production newProduction = new Production(nonTerms.get(i), rhs);
                            grammar.addProduction(newProduction);
                        }
                    }
                }
            }

            if (isRecursive(grammar, nonTerms.get(i))) {
                Set<Production> productionsNonTerminalI = grammar.findProductionsByLhs(nonTerms.get(i));
                for (Production production : productionsNonTerminalI) {
                    grammar.removeProduction(production);
                    if (production.getRhs().get(0).equals(nonTerms.get(i))) {
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.removeFirst();
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        grammar.addNonTerminals(newNonTerminal);
                        rhs.add(newNonTerminal);
                        Production updateOldProduction = new Production(newNonTerminal, rhs);

                        Terminal eps = Terminal.EPSILON;
                        grammar.addTerminals(eps);
                        Production newProduction = new Production(newNonTerminal, new LinkedList<>() {{add(eps);}});

                        grammar.addProduction(updateOldProduction);
                        grammar.addProduction(newProduction);
                    } else {
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        grammar.addNonTerminals(newNonTerminal);
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.remove(Terminal.EPSILON);
                        rhs.add(newNonTerminal);
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
            if (p.getRhs().contains(p.getLhs()))
                return true;
        }
        return false;
    }

    private static boolean isProductionLhsNonTerminalNextRhsNonTerminal(List<Production> productions, Symbol lhsNonTerminal, Symbol rhsNonTerminal) {
        for (Production production : productions) {
            if (production.getLhs().equals(lhsNonTerminal) && production.getRhs().contains(rhsNonTerminal))
                return true;
        }
        return false;
    }

    public static Grammar leftFactorization(Grammar grammar) {
       /* Set<Symbol> nonTerminals = grammar.getNonTerminals();

        for (Symbol nonTerminal : nonTerminals) {
            int count = 0;
            List<Production> productions = new ArrayList<>(grammar.findProductionsByLhs(nonTerminal));

            int length = 0;
            for (int i = 0; i < productions.size() - 1; i++) {
                List<Symbol> symbolsI = productions.get(i).getRhs();
                for (int j = i; j < productions.size(); j++) {
                    List<Symbol> symbolsJ = productions.get(j).getRhs();
                    int maxLength = 0;

                }
            }

        }
        */
        return null;
    }

    // TODO: провести рефакторинг и деббагинг, вроде бы работает, написать тесты
    public static Grammar removeLongRules(Grammar grammar) {
        NonTerminal startSymbol = new NonTerminal(grammar.getStartSymbol().getName());
        Grammar newGrammar = new Grammar(grammar.getName(), startSymbol);
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
                    newRhs.add(newNonTerminal);
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
        // TODO: Сделать удаление эпсилон-правил
        return null;
    }

    public static Grammar removeChainRules(Grammar grammar) {
        // TODO: Сделать удаление цепных правил
        return null;
    }

    public static Grammar removeUselessCharacter(Grammar grammar) {
        // TODO: Сделать удаление бесполезных символов
        return null;
    }

    public static Grammar removeMeetingSeveralTerminals(Grammar grammar) {
        // TODO: Сделать удаление встречи нескольких терминалов в одном правиле
        return null;
    }
/*
    public static Grammar conversionToChomskyNormalForm(Grammar grammar) {
        Symbol startTerminal = Symbol.builder()
                .type(grammar.getStartSymbol().getType())
                .name(grammar.getStartSymbol().getName())
                .build();
        Set<Production> productions = grammar.getProductions();

        Set<Production> newProductions = new HashSet<>();
        Set<Symbol> nonTerminals = new HashSet<>(grammar.getNonTerminals());
        Set<Symbol> terminals = new HashSet<>(grammar.getTerminals());

        for (Production production : productions) {
            Symbol lhs = production.getLhs();
            List<Symbol> rhs = production.getRhs();
            if ((rhs.size() == 1 || rhs.size() == 2) && rhs.get(0).getType() == Symbol.Type.TERM)
                newProductions.add(production);
            else if (lhs.equals(grammar.getStartSymbol()) && rhs.size() == 1 && rhs.get(0).getType() == Symbol.Type.EPS)
                newProductions.add(production);
            else if (rhs.size() == 2 && rhs.get(0).getType() == Symbol.Type.NON_TERM && rhs.get(1).getType() == Symbol.Type.NON_TERM)
                newProductions.add(production);

            if (rhs.size() > 2) {
                Symbol nonTerminal = production.getLhs();
                for (int i = 0; i < rhs.size() - 2; i++) {
                    Symbol newNonTerminal = Symbol.builder()
                            .type(Symbol.Type.NON_TERM)
                            .name(production.getLhs().getName() + i)
                            .build();
                    nonTerminals.add(newNonTerminal);
                    List<Symbol> newRhs = new ArrayList<>();

                    newRhs.add(rhs.get(i));
                    newRhs.add(newNonTerminal);
                    Production newProduction = Production.builder()
                            .lhs(nonTerminal)
                            .rhs(newRhs)
                            .build();
                    nonTerminal = newNonTerminal;
                    newProductions.add(newProduction);
                }
                Production prod = Production.builder()
                        .lhs(nonTerminal)
                        .rhs(new ArrayList<>() {{
                            add(rhs.get(rhs.size() - 2));
                            add(rhs.get(rhs.size() - 1));
                        }})
                        .build();
                newProductions.add(prod);

                newProductions.remove(production);
            }
        }

        Set<Production> resultProductions = new HashSet<>(newProductions);
        for (Production production : newProductions) {
            List<Symbol> rhs = production.getRhs();
            if (rhs.size() == 2) {
                if (rhs.get(0).getType() == Symbol.Type.TERM) {
                    Symbol nonTerminal = Symbol.builder()
                            .type(Symbol.Type.NON_TERM)
                            .spell(null)
                            .name(rhs.get(0).getSpell().toUpperCase() + "'")
                            .build();

                    nonTerminals.add(nonTerminal);

                    Production newProduction = Production.builder()
                            .lhs(nonTerminal)
                            .rhs(new ArrayList<>() {{
                                add(rhs.get(0));
                            }})
                            .build();
                    rhs.set(0, nonTerminal);
                    resultProductions.remove(production);
                    resultProductions.add(newProduction);
                }
                if (rhs.get(1).getType() == Symbol.Type.TERM) {
                    Symbol nonTerminal = Symbol.builder()
                            .type(Symbol.Type.NON_TERM)
                            .spell(null)
                            .name(rhs.get(1).getSpell().toUpperCase() + "'")
                            .build();

                    nonTerminals.add(nonTerminal);
                    Production newProduction = Production.builder()
                            .lhs(nonTerminal)
                            .rhs(new ArrayList<>() {{
                                add(rhs.get(1));
                            }})
                            .build();
                    rhs.set(1, nonTerminal);
                    resultProductions.remove(production);
                    resultProductions.add(newProduction);
                }
            }
        }

        grammar = removeEpsilonRules(grammar);
        grammar = removeChainRules(grammar);
        grammar = removeUselessCharacter(grammar);
        grammar = removeUselessCharacter(grammar);

        return Grammar.builder()
                .productions(resultProductions)
                .terminals(terminals)
                .nonTerminals(nonTerminals)
                .startSymbol(startTerminal)
                .name(grammar.getName())
                .build();
    }


*/
}
