package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class GrammarWithoutLeftRecursionBuilder {

    private static final Logger log = Logger.getLogger(GrammarWithoutLeftRecursionBuilder.class);

    /** Устранение левой рекурсии
     * @param grammar - грамматика без циклов и eps-продукций
     * @return грамматика без левой рекурсии
     * */
    public static Grammar build(Grammar grammar) {
        log.debug("Left Recursion Elimination");
        log.debug("Grammar Before:\n" + grammar);
        // Создаем новую грамматику на основе существующей
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        grammar.getProductions().forEach(production ->
            newGrammar.addProduction(production.getLhs(), production.getRhs().toArray(Symbol[]::new))
        );

        // Расположение нетерминалов в произвольном порядке (генерация из множества) NT1, NT2, NT3, ... NTn
        List<NonTerminal> nonTerms = new ArrayList<>(grammar.getNonTerminals());

        // Для каждого i от 0 до n - 1
        for (int i = 0, size = nonTerms.size(); i < size; i++) {
            // Для каждого j от 0 до i - 1
            for (int j = 0; j < i; j++) {

                // Поиск всех продукций NTi
                Set<Production> productionsNonTerminalI = newGrammar.findProductionsByLhs(nonTerms.get(i));
                for (Production pi : productionsNonTerminalI) {
                    // Для каждой такой продукции вида NTi -> NTj делать
                    if (pi.getRhs().get(0).equals(Symbol.of(nonTerms.get(j)))) {
                        List<Symbol> rhspi = new LinkedList<>(pi.getRhs());
                        newGrammar.removeProduction(pi);
                        rhspi.remove(0);

                        // Поиск всех продукций NTj
                        Set<Production> productionsNonTerminalJ = newGrammar.findProductionsByLhs(nonTerms.get(j));

                        // Заменяем данную продукцию продукциями NTi -> d1 y|d2 y|...|dk y, где NTj -> d1|d2|d3|...|dk
                        for (Production pj : productionsNonTerminalJ) {
                            List<Symbol> rhs = new LinkedList<>(pj.getRhs());
                            rhs.addAll(rhspi);
                            Production newProduction = new Production(nonTerms.get(i), rhs);
                            newGrammar.addProduction(newProduction);
                        }
                    }
                }
                log.debug("i = " + i + ", j = " + j + ". " + newGrammar.toString());
            }

            // Устранение непосредственной левой рекурсии среди NTi - продукций
            if (isRecursive(newGrammar, Symbol.of(nonTerms.get(i)))) {
                Set<Production> productionsNonTerminalI = newGrammar.findProductionsByLhs(nonTerms.get(i));
                // Проходимся по всем NTi - продукциями
                for (Production production : productionsNonTerminalI) {
                    newGrammar.removeProduction(production);
                    // Если продукция содержит в правой части на первом месте нетерминал из левой части,
                    // то добавляю заменяю правило NTi -> NTi y на NTi' - > y NTi' | eps
                    if (production.getRhs().get(0).equals(Symbol.of(nonTerms.get(i)))) {
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.removeFirst();
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        newGrammar.addNonTerminals(newNonTerminal);
                        rhs.add(Symbol.of(newNonTerminal));
                        Production updateOldProduction = new Production(newNonTerminal, rhs);

                        Terminal eps = Terminal.EPSILON;
                        newGrammar.addTerminals(eps);
                        Production newProduction = new Production(newNonTerminal, new LinkedList<>() {{
                            add(Symbol.of(eps));
                        }});

                        newGrammar.addProduction(updateOldProduction);
                        newGrammar.addProduction(newProduction);
                    // Иначе меняю заменяю правило NTi -> d на NTi -> d NTi'
                    } else {
                        NonTerminal newNonTerminal = new NonTerminal(nonTerms.get(i).getName() + "'");
                        newGrammar.addNonTerminals(newNonTerminal);
                        LinkedList<Symbol> rhs = new LinkedList<>(production.getRhs());
                        rhs.remove(Symbol.of(Terminal.EPSILON));
                        rhs.add(Symbol.of(newNonTerminal));
                        Production updateOldProduction = new Production(production.getLhs(), rhs);

                        newGrammar.addProduction(updateOldProduction);
                    }
                }
            }
            log.debug("i = " + i + ". " + newGrammar.toString());
        }
        log.debug("Grammar Result:\n" + newGrammar);
        return newGrammar;
    }

    /**
     * Проверка того, рекурсивно ли правило
     * @param grammar - грамматика, в которой содержится правило
     * @param nonTerm - нетерминал слева в правиле
     * @return true/false
     */
    private static boolean isRecursive(Grammar grammar, Symbol nonTerm) {
        Set<Production> productions = grammar.findProductionsByLhs(nonTerm.isNonTerminalGetting());
        for (Production p : productions) {
            if (p.getRhs().contains(Symbol.of(p.getLhs())))
                return true;
        }
        return false;
    }

}
