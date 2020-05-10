package ic7cc.ovchinnikov.lab2.optimization;

import ic7cc.ovchinnikov.lab2.model.*;
import ic7cc.ovchinnikov.lab2.util.Pair;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ChomskyNormalGrammarBuilder {

    /**
     * Преобразование к грамматике Хомского
     *
     * @param grammar - КС-грамматика
     * @return грамматика в нормальной форме Хомского
     */
    public static Grammar build(Grammar grammar) {
        log.info("Input Grammar (building chomsky normal grammar): {}", grammar);
        // Проверка того, что есть хотя бы одно правило со стартовым нетерминалом справа
        boolean isRightStartSymbol = false;
        for (Production production : grammar.getProductions()) {
            if (production.getRhs().contains(Symbol.of(grammar.getStartSymbol()))) {
                isRightStartSymbol = true;
                break;
            }
        }
        // Если такая продукция существует, то добавляем новый нетерминал S' и новое правило вида S' -> S,
        // на основе существующей грамматики генерю новую с новым стартовым символом и правилом
        if (isRightStartSymbol) {
            Grammar newGrammar = new Grammar(grammar.getName(), grammar.createNewNonTerminal(grammar.getStartSymbol().getName()).getName());
            NonTerminal nonTerminal = newGrammar.getStartSymbol();
            List<NonTerminal> nonTerminals = new LinkedList<>(grammar.getNonTerminals());
            newGrammar.addNonTerminals(nonTerminals.toArray(NonTerminal[]::new));
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addProduction(nonTerminal, Symbol.of(grammar.getStartSymbol()));
            for (Production production : grammar.getProductions()) {
                newGrammar.addProduction(production);
            }
            grammar = newGrammar;
        }
        Grammar newGrammar = removeMeetingSeveralTerminals(         // 5. Перенос терминалов в отдельные продукции
                removeUselessCharacter(                             // 4. Удаление бесполезных правил
                        removeChainRules(                           // 3. Удаление цепных правил
                                removeEpsilonRules(                 // 2. Удаление eps-правил (работает только для недлинных продукций)
                                        removeLongRules(grammar)    // 1. Удаление длинных правил
                                )
                        )
                )
        );
        log.info("Output Grammar (building chomsky normal grammar): {}", newGrammar);
        return newGrammar;
    }

    /**
     * Удаление длинных правил
     *
     * @param grammar - КС-грамматика
     * @return КС-грамматика без длинных правил (2 терминала и/или нетерминала справа)
     */
    public static Grammar removeLongRules(Grammar grammar) {
        log.info("Input Grammar (removing long rules): {}", grammar);
        // Инициализация новой грамматики с терминалами и нетерминалами существующей
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));

        // Проход по продукциям
        for (Production production : grammar.getProductions()) {
            newGrammar.addProduction(production);
            List<Symbol> rhs = production.getRhs();
            // Если правило длинное
            if (rhs.size() > 2) {
                NonTerminal nonTerminal = production.getLhs();

                // Создаем новые правила на основе длинного
                for (int i = 0; i < rhs.size() - 2; i++) {
                    NonTerminal newNonTerminal = newGrammar.createNewNonTerminal(production.getLhs().getName() + i);

                    newGrammar.addNonTerminals(newNonTerminal);
                    List<Symbol> newRhs = new ArrayList<>();

                    newRhs.add(rhs.get(i));
                    newRhs.add(Symbol.of(newNonTerminal));
                    Production newProduction = new Production(nonTerminal, newRhs);
                    // Сдвигаем ссылку на новосозданный нетерминал
                    nonTerminal = newNonTerminal;
                    newGrammar.addProduction(newProduction);
                }
                // Создаем последнее новое правило
                Production prod = new Production(nonTerminal, new LinkedList<>() {{
                    add(rhs.get(rhs.size() - 2));
                    add(rhs.get(rhs.size() - 1));
                }});
                newGrammar.addProduction(prod);

                newGrammar.removeProduction(production);
            }
        }
        log.info("Output Grammar (removing long rules): {}", newGrammar);
        return newGrammar;
    }

    /**
     * Удаление eps-правил (работает только для недлинных продукций)
     * @param grammar - КС-грамматика без длинных продукций
     * @return КС-грамматика без eps-правил
     */
    public static Grammar removeEpsilonRules(Grammar grammar) {
        log.info("Input Grammar (removing epsilon rules): {}", grammar);
        Map<NonTerminal, Boolean> isEpsilon = searchEpsilonRules(grammar);
        log.info("Epsilon NonTerminals: {}", isEpsilon);

        Grammar newGrammar;
        // Если стартовый символ является eps-порождающим, то создаю новую грамматику на основе существующей (без продукций)
        // и добавляю новое правило и терминал
        if (isEpsilon.get(grammar.getStartSymbol())) {
            newGrammar = new Grammar(grammar.getName(), grammar.createNewNonTerminal(grammar.getStartSymbol().getName() + "'").getName());
            NonTerminal newTerminalSp = newGrammar.getStartSymbol();
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addNonTerminals(newTerminalSp);
            newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
            newGrammar.addProduction(newTerminalSp, Symbol.of(grammar.getStartSymbol()));
            newGrammar.addProduction(newTerminalSp, Symbol.EPSILON);
        // иначе генерю грамматику без продукций
        } else {
            newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
            newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
            newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        }

        // Переписываю все продукции при условии, что они не eps и для тех, которые отвечают условиям (содержат хотя
        // один eps-порождающий нетерминал), добавляю новые правила
        for (Production production : grammar.getProductions()) {
            List<Symbol> rhs = production.getRhs();
            if (!rhs.contains(Symbol.EPSILON)) {
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
        log.info("Output Grammar (removing epsilon rules): {}", newGrammar);
        return newGrammar;
    }

    /**
     * Поиск всех eps-порождающих нетерминалов
     * @param grammar - КС-грамматика
     * @return мапа нетерминал - порождает (true) / не порождает (false)
     */
    public static Map<NonTerminal, Boolean> searchEpsilonRules(Grammar grammar) {
        log.info("Input Grammar (search epsilon rules): {}", grammar);
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
                if (symbol.isNonTerminal() || symbol.isTerminal())
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
                counter.put(number, counter.get(number) - (int) productionMap.get(number).getRhs().stream()
                        .filter(entry -> entry.equals(nonTerminal.toSymbol())).count());
                if (counter.get(number) == 0) {
                    NonTerminal nt = productionMap.get(number).getLhs();
                    if (!isEpsilon.get(nt)) {
                        queue.add(nt);
                        isEpsilon.put(nt, true);
                    }
                }
            }
        }
        log.info("Output Grammar (search epsilon rules): {}",
                Arrays.toString(isEpsilon.entrySet().stream().filter(Map.Entry::getValue).map(entry -> entry.getKey().getName()).toArray()));
        return isEpsilon;
    }

    /**
     * Продукция содержит только терминалы?
     * @param production - продукция
     * @return true/false
     */
    private static boolean isContainsOnlyTerminal(Production production) {
        for (Symbol symbol : production.getRhs()) {
            if (symbol.isNonTerminal() || symbol.isEpsilon())
                return false;
        }
        return true;
    }

    /**
     * Удаление цепных правил вида A -> B
     * @param grammar - КС-грамматика
     * @return КС-грамматика без цепных правил
     */
    public static Grammar removeChainRules(Grammar grammar) {
        log.info("Input Grammar (removing chain rules): {}", grammar);
        // Генерю для каждого нетерминала цепное правило вида A -> A (это требуется для поиска действительных цепных правил)
        Queue<Pair<NonTerminal, NonTerminal>> queue = new ArrayDeque<>();
        for (NonTerminal nonTerminal : grammar.getNonTerminals()) {
            queue.add(new Pair<>(nonTerminal, nonTerminal));
        }

        // По очереди просматриваю пары нетерминалов, образующих цепные правила
        Set<Pair<NonTerminal, NonTerminal>> set = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            Pair<NonTerminal, NonTerminal> pair = queue.remove();
            set.add(pair);
            for (Production production : grammar.getProductions()) {
                Pair<NonTerminal, NonTerminal> p = new Pair<>(pair.f(), production.getRhs().get(0).isNonTerminalGetting());
                // Если правило цепное и не содержится в множестве уже найденных цепных правил, то добавляю в очередь
                if (!set.contains(p) && production.getRhs().size() == 1 &&
                        production.getRhs().get(0).isNonTerminal() && pair.s().equals(production.getLhs())) {
                    queue.add(p);
                }
            }
        }

        // Генерю новую грамматику
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addNonTerminals(grammar.getNonTerminals().toArray(NonTerminal[]::new));
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        // Последовательно проверяю цепные правила и переписываю, только те, которые не являются цепными
        for (Pair<NonTerminal, NonTerminal> pair : set) {
            for (Production production : grammar.getProductions()) {
                if (!pair.s().equals(pair.f())) {
                    if (isNotChainRule(production) && production.getLhs().equals(pair.s())) {
                        newGrammar.addProduction(pair.f(), production.getRhs().toArray(Symbol[]::new));
                    }
                } else if (isNotChainRule(production)) {
                    newGrammar.addProduction(production);
                }
            }
        }
        log.info("Output Grammar (removing chain rules): {}", newGrammar);
        return newGrammar;
    }

    private static boolean isNotChainRule(Production production) {
        return production.getRhs().size() != 1 || !production.getRhs().get(0).isNonTerminal();
    }

    /**
     * Удаление бесполезных символов
     * @param grammar - КС грамматика
     * @return КС грамматика без бесполезных символов
     */
    public static Grammar removeUselessCharacter(Grammar grammar) {
        Grammar updateGrammar = removeNonGeneratingNonTerminals(grammar);
        return removeUnreachableNonTerminal(updateGrammar);
    }

    /**
     * Удаление непорождающих нетерминалов
     * @param grammar - КС грамматика
     * @return КС грамматика без непорождающих терминалов
     */
    public static Grammar removeNonGeneratingNonTerminals(Grammar grammar) {
        log.info("Input Grammar (removing non generating nonterminals): {}", grammar);
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

        // Ищу непорождаемые нетерминалы
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
        for (Map.Entry<NonTerminal, Boolean> entry : isGenerating.entrySet()) {
            if (entry.getValue())
                newGrammar.addNonTerminals(entry.getKey());
        }
        for (Production production : productions) {
            newGrammar.addProduction(production);
        }
        log.info("Output Grammar (removing non generating nonterminals): {}", newGrammar);
        return newGrammar;
    }

    /**
     * Удаление недостижимых символов
     * @param grammar - КС-грамматика
     * @return КС-грамматика без недостижимых символов
     */
    public static Grammar removeUnreachableNonTerminal(Grammar grammar) {
        log.info("Input Grammar (removing unreachable nonterminals): {}", grammar);

        Set<NonTerminal> nonTerminals = new HashSet<>();
        nonTerminals.add(grammar.getStartSymbol());
        Queue<NonTerminal> queue = new ArrayDeque<>();
        queue.add(grammar.getStartSymbol());
        // Прохожу по очереди, начиная от стартового символа, ищу все недостижимые и добавляю в множество только достижимые
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

        // Переписываю все продукции достижимых нетерминалов
        Set<Production> productions = new LinkedHashSet<>();
        for (Production production : grammar.getProductions()) {
            if (nonTerminals.contains(production.getLhs()))
                productions.add(production);
        }
        Grammar newGrammar = new Grammar(grammar.getName(), grammar.getStartSymbol().getName());
        newGrammar.addTerminals(grammar.getTerminals().toArray(Terminal[]::new));
        newGrammar.addNonTerminals(nonTerminals.toArray(NonTerminal[]::new));

        for (Production production : productions) {
            newGrammar.addProduction(production);
        }
        log.info("Output Grammar (removing unreachable nonterminals): {}", newGrammar);
        return newGrammar;
    }

    /**
     * Удаление тех правил, в которых встречаются несколько терминалов, и замена их новыми.
     * Работает только для правил, в которых в правой части содержатся 2 символа (хотя бы один символ терминал)
     * @param grammar - КС-грамматика, отвечающая вышепоставленному условию
     * @return КС-грамматика с правилами вида A -> BC A -> a B -> b C -> c и т.д.
     */
    public static Grammar removeMeetingSeveralTerminals(Grammar grammar) {
        log.info("Input Grammar (removing meeting several terminals): {}", grammar);

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
        log.info("Output Grammar (removing meeting several terminals): {}", newGrammar);
        return newGrammar;
    }

}
