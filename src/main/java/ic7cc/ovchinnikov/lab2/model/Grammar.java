package ic7cc.ovchinnikov.lab2.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.security.InvalidParameterException;
import java.util.*;

@EqualsAndHashCode
public class Grammar {

    private final String name;
    private final LinkedHashSet<Terminal> terminals;
    private final LinkedHashSet<NonTerminal> nonTerminals;
    private final Set<Production> productions;
    private final NonTerminal startSymbol;

    public Grammar(String name, String startSymbol) {
        this.name = name;
        this.terminals = new LinkedHashSet<>();
        this.nonTerminals = new LinkedHashSet<>();
        this.productions = new LinkedHashSet<>();
        this.startSymbol = new NonTerminal(startSymbol);
        this.nonTerminals.add(this.startSymbol);
    }

    @JsonCreator
    private Grammar(@JsonProperty("name") String name, @JsonProperty("startSymbol") NonTerminal startSymbol,
                   @JsonProperty("terminals") LinkedHashSet<Terminal> terminals, @JsonProperty("nonTerminals") LinkedHashSet<NonTerminal> nonTerminals,
                   @JsonProperty("productions") Set<Production> productions) {
        this.name = name;
        this.terminals = terminals;
        this.nonTerminals = nonTerminals;
        this.productions = productions;
        this.startSymbol = startSymbol;
    }

    public boolean addTerminals(Terminal... terminals) {
        return this.terminals.addAll(Arrays.asList(terminals));
    }

    public boolean addNonTerminals(NonTerminal... nonTerminals) {
        return this.nonTerminals.addAll(Arrays.asList(nonTerminals));
    }

    public boolean addProduction(NonTerminal lhs, Symbol... rhs) {
        if (!nonTerminals.contains(lhs))
            throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal  {"+lhs+"}");
        for (Symbol symbol : rhs) {
            if (symbol.isTerminal()) {
                if (!terminals.contains(symbol.isTerminalGetting()))
                    throw new InvalidParameterException("Terminals doesnt contain Terminal {"+symbol+"}");
            } else if (symbol.isNonTerminal()) {
                if (!nonTerminals.contains(symbol.isNonTerminalGetting()))
                    throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal {"+symbol+"}");
            }
        }
        List<Symbol> resultRhs = new LinkedList<>(Arrays.asList(rhs));
        for (Symbol symbol : rhs) {
            if (symbol.isEpsilon()) {
                resultRhs.remove(symbol);
            }
        }
        if (resultRhs.size() == 0)
            resultRhs.add(Symbol.of(Terminal.EPSILON));

        return productions.add(new Production(lhs, resultRhs));
    }

    public boolean addProduction(Production production) {
        if (!nonTerminals.contains(production.getLhs()))
            throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal  {"+production.getLhs()+"}");
        for (Symbol symbol : production.getRhs()) {
            if (symbol.isTerminal()) {
                if (!terminals.contains(symbol.isTerminalGetting()))
                    throw new InvalidParameterException("Terminals doesnt contain Terminal {"+symbol+"}");
            } else if (symbol.isNonTerminal()) {
                if (!nonTerminals.contains(symbol.isNonTerminalGetting()))
                    throw new InvalidParameterException("NonTerminals doesnt contain NonTerminal {"+symbol+"}");
            }
        }
        return productions.add(new Production(production.getLhs(), new LinkedList<>(production.getRhs())));
    }

    public NonTerminal createNewNonTerminal(String name) {
        if (nonTerminals.contains(new NonTerminal(name))) {
            return createNewNonTerminal(name + "'");
        }
        return new NonTerminal(name);
    }

    public boolean removeProduction(Production production) {
        return productions.remove(production);
    }

    public String getName() {
        return name;
    }

    public Set<Terminal> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public Set<NonTerminal> getNonTerminals() {
        return Collections.unmodifiableSet(nonTerminals);
    }

    public Set<Production> getProductions() {
        return Collections.unmodifiableSet(productions);
    }

    public NonTerminal getStartSymbol() {
        return startSymbol;
    }

    public Set<Production> findProductionsByLhs(NonTerminal lhs) {
        Set<Production> resultProductions = new HashSet<>();
        for (Production production : productions) {
            if (production.getLhs().equals(lhs))
                resultProductions.add(production);
        }
        return resultProductions;
    }

    @JsonIgnore
    public boolean isValid() {
        if (terminals.isEmpty() || nonTerminals.isEmpty() || productions.isEmpty())
            return false;
        if (startSymbol == null)
            return false;
        return nonTerminals.contains(startSymbol);
    }

    @Override
    public String toString() {
        StringBuilder productionString = new StringBuilder();
        for (Production production : productions) {
            productionString.append("\t\t").append(production.getLhs().getName()).append(" -> ");
            for (Symbol symbol : production.getRhs())
                productionString.append(symbol.isTerminal() ? symbol.getSpell() : symbol.getName()).append(" ");
            productionString.append("\n");
        }

        return "Grammar {\n" +
                    "\tStartSymbol: " + startSymbol.getName() + "\n" +
                    "\tTerminals: " + Arrays.toString(terminals.stream().map(Terminal::getName).toArray()) + "\n" +
                    "\tNonTerminals: " + Arrays.toString(nonTerminals.stream().map(NonTerminal::getName).toArray()) + "\n" +
                    "\tProductions: \n" + productionString +
                "}";

    }
}
