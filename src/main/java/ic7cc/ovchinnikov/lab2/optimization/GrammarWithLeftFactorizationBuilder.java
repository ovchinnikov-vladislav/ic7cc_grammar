package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;
import org.apache.log4j.Logger;

import java.util.*;

public class GrammarWithLeftFactorizationBuilder {

    private static final Logger log = Logger.getLogger(GrammarWithLeftFactorizationBuilder.class);

    /** Проведение левой факторизации
     * @param grammar - грамматика
     * @return левофакторизованная грамматика
     * */
    public static Grammar leftFactorization(Grammar grammar) {
        log.debug("Left Factorization");
        log.debug("Input: " + grammar);
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));

        // Есть ли возможность провести левую факторизацию?
        boolean isPossibleLeftFactorization = true;
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            if (findAllPrefixOfNonTerminal(grammar, nonTerminal).size() > 0) {
                isPossibleLeftFactorization = false;
            }
        }
        if (isPossibleLeftFactorization) {
            log.debug("Grammar Result: " + grammar);
            return grammar;
        }

        // Для каждого нетерминала
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            // Находим все возмсожные префиксы для данного нетерминала
            Map<List<Symbol>, Set<Production>> map = findAllPrefixOfNonTerminal(grammar, nonTerminal);
            List<Symbol> symbols = null;
            int length = 0;
            // Ищем самый длинный префикс из найденных
            for (Map.Entry<List<Symbol>, Set<Production>> entry : map.entrySet()) {
                if (entry.getKey().size() > length) {
                    length = entry.getKey().size();
                    symbols = entry.getKey();
                }
            }

            // Если такой префикс есть
            if (symbols != null) {
                // Получаем все продукции с этим префиксом
                Set<Production> productions = map.get(symbols);
                int i = 0;
                NonTerminal newNonTerminal = null;

                // Проходимся по всем продукциям
                for (Production production : grammar.getProductions()) {
                    // Если данная продукция не содержит данный префикс, то просто перенесем ее в новую грамматику
                    if (!productions.contains(production)) {
                        newGrammar.addProduction(production);
                    // Иначе делаем проход по этой продукции и префиксу с целью выполнения левой факторизации
                    } else {
                        // Итератор правой части продукции из грамматики
                        Iterator<Symbol> iteratorProdRhs = production.getRhs().iterator();
                        // Итератор префикса
                        Iterator<Symbol> iteratorPrefix = symbols.iterator();
                        List<Symbol> newRhs = new LinkedList<>();
                        // Если это первая итерации для нового правила, то создаем нетерминал, отличный от других
                        if (i == 0) {
                            i++;
                            newNonTerminal = newGrammar.createNewNonTerminal(production.getLhs().getName() + "_LF");
                        }
                        // Пока символы в итераторе префикса и итераторе продукции есть, делать
                        while (iteratorPrefix.hasNext()) {
                            if (iteratorProdRhs.hasNext()) {
                                Symbol symbolPrefix = iteratorPrefix.next();
                                Symbol symbolProdRhs = iteratorProdRhs.next();
                                // Символы префикса и символ продукции равны, то добавить в новое правило
                                if (symbolPrefix.equals(symbolProdRhs))
                                    newRhs.add(symbolPrefix);
                                // иначе пошло отличие, завершить проход
                                else
                                    break;
                            }
                        }
                        List<Symbol> rhsNewProduction = new LinkedList<>();
                        // В итераторе продукции остались символы? Переписать.
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

        // Рекурсивно повторить левую факторизацию
        return leftFactorization(newGrammar);
    }

    /**
     * Поиск префиксов
     * @param grammar - грамматика
     * @param nonTerminal - нетерминал, для которого требуется найти все префиксы из правой части
     * @return мапа ключ - префикс, значение - множество продукций, соответствующих данному префиксу
     */
    private static Map<List<Symbol>, Set<Production>> findAllPrefixOfNonTerminal(Grammar grammar, NonTerminal nonTerminal) {

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

}
